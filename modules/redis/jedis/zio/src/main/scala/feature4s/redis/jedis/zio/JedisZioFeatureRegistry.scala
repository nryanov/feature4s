package feature4s.redis.jedis.zio

import feature4s.effect.zio.ZioMonadError
import feature4s.redis.jedis.JedisFeatureRegistry
import redis.clients.jedis.Jedis
import redis.clients.jedis.util.Pool
import zio.Task
import zio.blocking.Blocking

class JedisZioFeatureRegistry private (
  pool: Pool[Jedis],
  namespace: String,
  blocking: Blocking.Service
) extends JedisFeatureRegistry[Task](
      pool = pool,
      namespace = namespace,
      monad = new ZioMonadError(blocking)
    )

object JedisZioFeatureRegistry {
  def useClient(
    pool: Pool[Jedis],
    namespace: String,
    blocking: Blocking.Service
  ): JedisZioFeatureRegistry = new JedisZioFeatureRegistry(pool, namespace, blocking)
}
