package feature4s.redis.jedis.cats

import cats.effect.{Blocker, ContextShift, Sync}
import feature4s.effect.cats.CatsMonadError
import feature4s.redis.jedis.JedisFeatureRegistry
import redis.clients.jedis.JedisPool

class JedisCatsFeatureRegistry[F[_]: ContextShift: Sync] private (
  pool: JedisPool,
  namespace: String,
  blocker: Blocker
) extends JedisFeatureRegistry[F](
      pool = pool,
      namespace = namespace,
      monad = new CatsMonadError[F](blocker)
    )

object JedisCatsFeatureRegistry {
  def useClient[F[_]: ContextShift: Sync](
    pool: JedisPool,
    namespace: String,
    blocker: Blocker
  ): JedisCatsFeatureRegistry[F] = new JedisCatsFeatureRegistry(pool, namespace, blocker)
}
