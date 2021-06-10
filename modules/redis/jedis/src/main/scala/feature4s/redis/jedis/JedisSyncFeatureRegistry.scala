package feature4s.redis.jedis

import feature4s.Id
import feature4s.monad.IdMonadError
import redis.clients.jedis.JedisPool

class JedisSyncFeatureRegistry private (pool: JedisPool, namespace: String)
    extends JedisFeatureRegistry[Id](pool = pool, namespace = namespace, monad = IdMonadError)

object JedisSyncFeatureRegistry {
  def apply(pool: JedisPool, namespace: String): JedisSyncFeatureRegistry =
    new JedisSyncFeatureRegistry(pool, namespace)
}
