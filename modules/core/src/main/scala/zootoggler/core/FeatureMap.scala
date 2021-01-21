package zootoggler.core

import java.util.concurrent.{CountDownLatch, ExecutorService, Executors, TimeUnit, TimeoutException}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.curator.framework.recipes.cache.{ChildData, CuratorCache, CuratorCacheListener}
import org.slf4j.{Logger, LoggerFactory}
import zootoggler.core.FeatureMap.FeatureInfo

import scala.collection.concurrent
import scala.collection.concurrent.TrieMap

private[core] class FeatureMap(cache: CuratorCache, featureRegisterTimeoutMs: Int) {
  private val logger: Logger = LoggerFactory.getLogger(FeatureMap.getClass)

  private val knownFeatures: concurrent.Map[String, FeatureInfo[_]] =
    TrieMap[String, FeatureInfo[_]]()

  private val listenerExecutor: ExecutorService =
    Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("zk-client-event-listener-%d")
        .build()
    )

  private def cacheListener(): CuratorCacheListener =
    CuratorCacheListener
      .builder()
      .forDeletes { (el: ChildData) =>
        logger.info(s"Path ${el.getPath} was deleted")
        knownFeatures.-=(el.getPath)
      }
      .build()

  cache.listenable().addListener(cacheListener(), listenerExecutor)

  def register[A](name: String, fullPath: String, action: => Attempt[Any])(implicit
    ft: FeatureType[A]
  ): Attempt[Unit] = {
    val latch = new CountDownLatch(1)
    val listener = featureCreationListener(latch, fullPath)
    for {
      _ <- Attempt.delay(cache.listenable().addListener(listener, listenerExecutor))
      _ <- action
      waitResult <- Attempt.delay(latch.await(featureRegisterTimeoutMs, TimeUnit.MILLISECONDS))
      _ <- Attempt.delay(cache.listenable.removeListener(listener))
      _ <-
        if (waitResult) {
          Attempt.successful(knownFeatures.+=(fullPath -> FeatureInfo(name, ft)))
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
    } yield ()
  }

  def update[A](name: String, fullPath: String, action: => Attempt[Boolean])(implicit
    ft: FeatureType[A]
  ): Attempt[Boolean] =
    for {
      currentType <- Attempt.fromOption(knownFeatures.get(fullPath))
      _ <-
        if (currentType.featureType.typeName != ft.typeName)
          Attempt.failure(
            new IllegalArgumentException("Attempt to update feature with value of another type")
          )
        else Attempt.successful(())
      result <- action
    } yield result

  def featureList(): Map[String, (Array[Byte], FeatureInfo[_])] =
    knownFeatures.map { case (path, info) =>
      (Option(cache.get(path).map(_.getData).orElse(null)), info)
    }.collect {
      case (value, info) if value.isDefined => (info.name, (value.get, info))
    }.toMap

  private def featureCreationListener(
    latch: CountDownLatch,
    path: String
  ): CuratorCacheListener =
    CuratorCacheListener
      .builder()
      .forCreates { (el: ChildData) =>
        if (el.getPath == path) {
          latch.countDown()
          logger.info(s"Path $path created")
        }
      }
      .build()
}

object FeatureMap {
  final case class FeatureInfo[A](name: String, featureType: FeatureType[A])

  final case class FeatureView(cachedValue: Array[Byte], featureType: FeatureType[_]) {
    val typeName: String = featureType.typeName

    val value: Attempt[String] = featureType.prettyPrint(cachedValue)
  }

  def apply(cache: CuratorCache, featureRegisterTimeoutMs: Int): FeatureMap =
    new FeatureMap(cache, featureRegisterTimeoutMs)
}
