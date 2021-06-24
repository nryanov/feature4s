package feature4s.redis.jedis

import feature4s.Id
import feature4s.monad.IdMonadError
import redis.clients.jedis.JedisCluster

class JedisClusterSyncFeatureRegistry private (jedisCluster: JedisCluster, namespace: String)
    extends JedisClusterFeatureRegistry[Id](
      jedisCluster = jedisCluster,
      namespace = namespace,
      monad = IdMonadError
    )

object JedisClusterSyncFeatureRegistry {
  def useClient(jedisCluster: JedisCluster, namespace: String): JedisClusterSyncFeatureRegistry =
    new JedisClusterSyncFeatureRegistry(jedisCluster, namespace)
}
