package feature4s.redis.jedis.cats

import cats.effect.Sync
import feature4s.effect.cats.CatsMonadError
import feature4s.redis.jedis.JedisClusterFeatureRegistry
import redis.clients.jedis.JedisCluster

class JedisClusterCatsFeatureRegistry[F[_]: ContextShift: Sync] private (
  jedisCluster: JedisCluster,
  namespace: String,
  blocker: Blocker
) extends JedisClusterFeatureRegistry[F](
      jedisCluster = jedisCluster,
      namespace = namespace,
      monad = new CatsMonadError[F](blocker)
    )

object JedisClusterCatsFeatureRegistry {
  def useClient[F[_]: ContextShift: Sync](
    jedisCluster: JedisCluster,
    namespace: String): JedisClusterCatsFeatureRegistry[F] =
    new JedisClusterCatsFeatureRegistry(jedisCluster, namespace, blocker)
}
