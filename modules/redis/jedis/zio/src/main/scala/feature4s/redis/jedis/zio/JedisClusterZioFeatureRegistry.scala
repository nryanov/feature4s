package feature4s.redis.jedis.zio

import feature4s.effect.zio.ZioMonadError
import feature4s.redis.jedis.JedisClusterFeatureRegistry
import redis.clients.jedis.JedisCluster
import zio.Task
import zio.blocking.Blocking

class JedisClusterZioFeatureRegistry private (
  jedisCluster: JedisCluster,
  namespace: String,
  blocking: Blocking.Service
) extends JedisClusterFeatureRegistry[Task](
      jedisCluster = jedisCluster,
      namespace = namespace,
      monad = new ZioMonadError(blocking)
    )

object JedisClusterZioFeatureRegistry {
  def useClient(
    jedisCluster: JedisCluster,
    namespace: String,
    blocking: Blocking.Service
  ): JedisClusterZioFeatureRegistry =
    new JedisClusterZioFeatureRegistry(jedisCluster, namespace, blocking)
}
