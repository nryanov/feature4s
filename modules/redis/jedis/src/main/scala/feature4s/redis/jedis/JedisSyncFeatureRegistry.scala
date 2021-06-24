package feature4s.redis.jedis

import feature4s.Id
import feature4s.monad.IdMonadError
import redis.clients.jedis.util.Pool
import redis.clients.jedis.Jedis

class JedisSyncFeatureRegistry private (pool: Pool[Jedis], namespace: String)
    extends JedisFeatureRegistry[Id](pool = pool, namespace = namespace, monad = IdMonadError)

object JedisSyncFeatureRegistry {
  def useClient(pool: Pool[Jedis], namespace: String): JedisSyncFeatureRegistry =
    new JedisSyncFeatureRegistry(pool, namespace)
}
