package feature4s.zookeeper.cats

import java.util.concurrent.{CountDownLatch, TimeUnit}

import cats.effect.{Blocker, ContextShift, Sync}
import cats.syntax.functor._
import cats.syntax.flatMap._
import feature4s.ClientError
import feature4s.effect.cats.CatsMonadError
import feature4s.monad.MonadError
import feature4s.zookeeper.{FeatureCacheMap, ZookeeperFeatureRegistry}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.CuratorCache

import scala.concurrent.duration._

class ZookeeperCatsFeatureRegistry[F[_]: ContextShift: Sync] private (
  client: CuratorFramework,
  cache: CuratorCache,
  cacheMap: FeatureCacheMap[F],
  zNode: String,
  monad: MonadError[F]
) extends ZookeeperFeatureRegistry[F](
      client = client,
      cache = cache,
      cacheMap = cacheMap,
      zNode = zNode,
      monad = monad
    )

object ZookeeperCatsFeatureRegistry {
  def useClient[F[_]: ContextShift](
    client: CuratorFramework,
    zNode: String,
    blocker: Blocker,
    cacheInitializationTimeout: Duration = 10.seconds
  )(implicit F: Sync[F]): F[ZookeeperCatsFeatureRegistry[F]] =
    for {
      _ <- F.whenA(client.getState != CuratorFrameworkState.STARTED)(
        F.raiseError(
          ClientError(
            new IllegalStateException(
              """Zookeeper client should be started
          |>> client.start()
          |""".stripMargin
            )
          )
        )
      )
      monad = new CatsMonadError[F](blocker)
      latch = new CountDownLatch(1)
      cache <- F.delay(CuratorCache.build(client, zNode))
      featureCacheMap = FeatureCacheMap(
        client = client,
        monad = monad,
        zNode = zNode,
        latch = latch
      )
      _ <- F.delay(cache.listenable().addListener(featureCacheMap))
      _ <- F.delay(cache.start())
      _ <- blocker.delay(latch.await(cacheInitializationTimeout.toSeconds, TimeUnit.SECONDS))
    } yield new ZookeeperCatsFeatureRegistry(
      client = client,
      cache = cache,
      cacheMap = featureCacheMap,
      zNode = zNode,
      monad = monad
    )
}
