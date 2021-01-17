package zootoggler.core

import java.util.concurrent.{CountDownLatch, ExecutorService, Executors, TimeUnit, TimeoutException}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.{Logger, LoggerFactory}
import zootoggler.core.configuration.ZtConfiguration
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.KeeperException.{
  BadVersionException,
  NoNodeException,
  NodeExistsException
}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.recipes.cache.{ChildData, CuratorCache, CuratorCacheListener}

private class ZtClientBasic(
  client: CuratorFramework,
  cache: CuratorCache,
  rootPath: String,
  featureRegisterTimeoutMs: Int
) extends ZtClient[Attempt] {
  private val logger: Logger = LoggerFactory.getLogger("ZtClient")

  private val listenerExecutor: ExecutorService =
    Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("zk-client-event-listener-%d")
        .build()
    )

  override def register[A](
    defaultValue: A,
    name: String,
    description: Option[String]
  )(implicit converter: Converter[A]): Attempt[FeatureAccessor[Attempt, A]] = {
    val fullPath = s"$rootPath/$name"

    logger.info(s"Register feature: $name")
    val result = for {
      value <- converter.toByteArray(defaultValue)
      latch = new CountDownLatch(1)
      listener = cacheListener(latch, fullPath)
      _ <- Attempt.delay(cache.listenable().addListener(listener, listenerExecutor))
      _ <- Attempt.delay(client.create().creatingParentsIfNeeded().forPath(fullPath, value))
      waitResult <- Attempt.delay(latch.await(featureRegisterTimeoutMs, TimeUnit.MILLISECONDS))
      _ <- Attempt.delay(cache.listenable.removeListener(listener))
      feature <-
        if (waitResult) {
          Attempt.successful(featureAccessor(Feature(defaultValue, name, description), fullPath))
        } else {
          logger.warn(
            s"Feature register timeout exceeded $featureRegisterTimeoutMs ms when registering new feature $name"
          )
          Attempt.failure(
            new TimeoutException(
              s"Feature register timeout exceeded $featureRegisterTimeoutMs ms when registering new feature $name"
            )
          )
        }
    } yield feature

    result.catchSome { case _: NodeExistsException =>
      logger.info(s"Feature $name is already registered")
      Attempt.successful(featureAccessor(Feature(defaultValue, name, description), fullPath))
    }.tapBoth(
      err => logger.error(s"Error happened while register feature: $name", err),
      _ => logger.info(s"Feature $name was successfully registered")
    )
  }

  override def remove(name: String): Attempt[Boolean] = {
    logger.info(s"Delete feature: $name")
    val fullPath = s"$rootPath/$name"
    Attempt
      .delay(client.delete().guaranteed().forPath(fullPath))
      .map(_ => true)
      .catchSome { case _: NoNodeException =>
        Attempt.successful(false)
      }
      .tapBoth(
        err => logger.error(s"Error happened while deleting feature: $name", err),
        _ => logger.info(s"Feature $name was successfully deleted")
      )
  }

  override def isExist(name: String): Attempt[Boolean] = {
    logger.info(s"Check if feature $name is exist")
    val fullPath = s"$rootPath/$name"
    Attempt
      .delay(client.checkExists().forPath(fullPath))
      .map { stat =>
        Option(stat) match {
          case Some(_) => true
          case None    => false
        }
      }
      .tapBoth(
        err => logger.error(s"Error happened while checking feature $name existence", err),
        _ => ()
      )
  }

  override def recreate[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Attempt[FeatureAccessor[Attempt, A]] = {
    logger.info(s"Recreate feature: $name")
    val fullPath = s"$rootPath/$name"
    val stat = new Stat()

    for {
      result <- Attempt.delay(client.getData.storingStatIn(stat).forPath(fullPath))
      version <- Attempt.fromOption(Option(result)).map(_ => stat.getVersion)
      _ <- Attempt
        .delay(client.delete().guaranteed().withVersion(version).forPath(fullPath))
        .map(_ => true)
        .catchSome { case _: NoNodeException =>
          Attempt.successful(false)
        }
      feature <- register(defaultValue, name, description)
    } yield feature
  }

  override def close(): Attempt[Unit] = {
    logger.debug("Closing zookeeper client")
    Attempt.delay(client.close())
  }.tapBoth(
    err => logger.error("Error happened while closing client", err),
    _ => logger.debug("Client was closed successfully")
  )

  private def featureAccessor[A](
    feature: Feature[A],
    fullPath: String
  )(implicit converter: Converter[A]): FeatureAccessor[Attempt, A] =
    new FeatureAccessor[Attempt, A] {
      override def value: Attempt[A] = for {
        element <- Attempt.fromOption(Option(cache.get(fullPath).orElse(null)))
        currentValue <- converter.fromByteArray(element.getData)
      } yield currentValue

      override def update(newValue: A): Attempt[Boolean] = {
        val stat = new Stat()
        val request = for {
          newData <- converter.toByteArray(newValue)
          result <- Attempt.delay(client.getData.storingStatIn(stat).forPath(fullPath))
          version <- Attempt.fromOption(Option(result)).map(_ => stat.getVersion)
          _ <- Attempt.delay(client.setData().withVersion(version).forPath(fullPath, newData))
        } yield true

        request.catchSome { case _: BadVersionException =>
          Attempt.successful(false)
        }.tapBoth(
          err => logger.error(s"Error happened while updating feature: $feature", err),
          _ => ()
        )
      }
    }

  private def cacheListener(
    latch: CountDownLatch,
    path: String
  ): CuratorCacheListener =
    CuratorCacheListener
      .builder()
      .forCreates { (el: ChildData) =>
        if (el.getPath == path) {
          latch.countDown()
          logger.info(s"Path $path created")
        } else {
          logger.info(s"PATH: $path")
        }
      }
      .build()
}

object ZtClientBasic {
  def apply(cfg: ZtConfiguration): ZtClient[Attempt] = {
    val client = CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy)

    client.start()
    client.blockUntilConnected(cfg.connectionTimeoutMs, TimeUnit.MILLISECONDS)

    val clientFacade = cfg.namespace match {
      case Some(value) => client.usingNamespace(value)
      case None        => client
    }

    val cache = CuratorCache.build(clientFacade, cfg.rootPath)

    val latch = new CountDownLatch(1)
    val cacheListener =
      CuratorCacheListener.builder().forInitialized(() => latch.countDown()).build()

    cache.listenable().addListener(cacheListener)
    cache.start()
    latch.await()
    cache.listenable().removeListener(cacheListener)

    new ZtClientBasic(
      clientFacade,
      cache,
      cfg.rootPath,
      cfg.featureRegisterTimeoutMs
    )
  }
}
