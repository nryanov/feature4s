package zootoggler.core

import java.util.concurrent.{CountDownLatch, TimeUnit}

import org.slf4j.{Logger, LoggerFactory}
import zootoggler.core.configuration.ZtConfiguration
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.KeeperException.{
  BadVersionException,
  NoNodeException,
  NodeExistsException
}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.recipes.cache.{CuratorCache, CuratorCacheListener}

private class ZtClientBasic(
  client: CuratorFramework,
  cache: CuratorCache,
  rootPath: String,
  featureRegisterTimeoutMs: Int
) extends ZtClient[Attempt] {
  private val logger: Logger = LoggerFactory.getLogger("ZtClient")
  private val featureMap: FeatureMap = FeatureMap(cache, featureRegisterTimeoutMs)

  override def register[A](
    defaultValue: A,
    name: String,
    description: Option[String]
  )(implicit ft: FeatureType[A]): Attempt[FeatureAccessor[Attempt, A]] = {
    val fullPath = s"$rootPath/$name"

    logger.info(s"Register feature: $name")
    val result = for {
      value <- ft.converter.toByteArray(defaultValue)
      _ <- featureMap.register(
        name,
        fullPath,
        Attempt.delay(client.create().creatingParentsIfNeeded().forPath(fullPath, value))
      )
    } yield featureAccessor(Feature(defaultValue, name, description), fullPath)

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

  override def recreate[A: FeatureType](
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

  override def update[A](name: String, newValue: A)(implicit
    ft: FeatureType[A]
  ): Attempt[Boolean] = {
    val fullPath = s"$rootPath/$name"
    val stat = new Stat()
    val request = for {
      newData <- ft.converter.toByteArray(newValue)
      result <- Attempt.delay(client.getData.storingStatIn(stat).forPath(fullPath))
      version <- Attempt.fromOption(Option(result)).map(_ => stat.getVersion)
      _ <- Attempt.delay(client.setData().withVersion(version).forPath(fullPath, newData))
    } yield true

    featureMap
      .update(name, fullPath, request)
      .catchSome { case _: BadVersionException =>
        Attempt.successful(false)
      }
      .tapBoth(
        err => logger.error(s"Error happened while updating feature: $name", err),
        _ => ()
      )
  }

  private def featureAccessor[A](
    feature: Feature[A],
    fullPath: String
  )(implicit ft: FeatureType[A]): FeatureAccessor[Attempt, A] =
    new FeatureAccessor[Attempt, A] {
      override def value: Attempt[A] = for {
        element <- Attempt.fromOption(Option(cache.get(fullPath).orElse(null)))
        currentValue <- ft.converter.fromByteArray(element.getData)
      } yield currentValue
    }
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
