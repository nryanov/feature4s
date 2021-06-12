package feature4s.zookeeper

import java.util.concurrent.{CountDownLatch, TimeUnit}

import feature4s.{ClientError, Id}
import feature4s.monad.IdMonadError
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.framework.recipes.cache.CuratorCache

import scala.concurrent.duration._

class ZookeeperSyncFeatureRegistry(
  client: CuratorFramework,
  cache: CuratorCache,
  featureCacheMap: FeatureCacheMap[Id],
  zNode: String
) extends ZookeeperFeatureRegistry[Id](
      client = client,
      cache = cache,
      cacheMap = featureCacheMap,
      zNode = zNode,
      monad = IdMonadError
    )

object ZookeeperSyncFeatureRegistry {
  def apply(
    client: CuratorFramework,
    zNode: String,
    cacheInitializationTimeout: Duration = 10.seconds
  ): ZookeeperSyncFeatureRegistry = {
    if (client.getState != CuratorFrameworkState.STARTED) {
      IdMonadError.raiseError(
        ClientError(new IllegalStateException("""
            |Zookeeper client should be started
            |>> client.start()
            |""".stripMargin))
      )
    }

    val latch = new CountDownLatch(1)
    val cache = CuratorCache.build(client, zNode)
    val featureCacheMap =
      FeatureCacheMap(client = client, monad = IdMonadError, zNode = zNode, latch = latch)

    cache.listenable().addListener(featureCacheMap)
    cache.start()

    latch.await(cacheInitializationTimeout.toSeconds, TimeUnit.SECONDS)

    new ZookeeperSyncFeatureRegistry(
      client = client,
      cache = cache,
      featureCacheMap = featureCacheMap,
      zNode = zNode
    )
  }
}
