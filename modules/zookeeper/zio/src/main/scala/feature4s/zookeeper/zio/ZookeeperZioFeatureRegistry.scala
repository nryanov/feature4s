package feature4s.zookeeper.zio

import java.util.concurrent.{CountDownLatch, TimeUnit}

import feature4s.ClientError
import feature4s.effect.zio.ZioMonadError
import feature4s.monad.MonadError
import feature4s.zookeeper.{FeatureCacheMap, ZookeeperFeatureRegistry}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.CuratorCache
import zio.blocking.Blocking
import zio.{Task, ZIO}

import scala.concurrent.duration._

class ZookeeperZioFeatureRegistry private (
  client: CuratorFramework,
  cache: CuratorCache,
  cacheMap: FeatureCacheMap[Task],
  zNode: String,
  monad: MonadError[Task]
) extends ZookeeperFeatureRegistry[Task](
      client = client,
      cache = cache,
      cacheMap = cacheMap,
      zNode = zNode,
      monad = monad
    )

object ZookeeperZioFeatureRegistry {
  def useClient(
    client: CuratorFramework,
    zNode: String,
    cacheInitializationTimeout: Duration = 10.seconds
  ): ZIO[Blocking, Throwable, ZookeeperZioFeatureRegistry] =
    for {
      _ <- Task.when(client.getState != CuratorFrameworkState.STARTED)(
        Task.fail(
          ClientError(new IllegalStateException("""
                                                  |Zookeeper client should be started
                                                  |>> client.start()
                                                  |""".stripMargin))
        )
      )
      blocking <- ZIO.service[Blocking.Service]
      monad = new ZioMonadError(blocking)
      latch = new CountDownLatch(1)
      cache <- Task.effect(CuratorCache.build(client, zNode))
      featureCacheMap = FeatureCacheMap(
        client = client,
        monad = monad,
        zNode = zNode,
        latch = latch
      )
      _ <- Task.effectTotal(cache.listenable().addListener(featureCacheMap))
      _ <- Task.effect(cache.start())
      _ <- blocking.effectBlocking(
        latch.await(cacheInitializationTimeout.toSeconds, TimeUnit.SECONDS)
      )
    } yield new ZookeeperZioFeatureRegistry(
      client = client,
      cache = cache,
      cacheMap = featureCacheMap,
      zNode = zNode,
      monad = monad
    )
}
