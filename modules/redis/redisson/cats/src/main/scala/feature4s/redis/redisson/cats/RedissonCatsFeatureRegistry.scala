package feature4s.redis.redisson.cats

import cats.effect.{Blocker, ContextShift, Sync}
import feature4s.effect.cats.CatsMonadError
import feature4s.redis.redisson.RedissonFeatureRegistry
import org.redisson.api.RedissonClient

class RedissonCatsFeatureRegistry[F[_]: ContextShift: Sync] private (
  client: RedissonClient,
  namespace: String,
  blocker: Blocker
) extends RedissonFeatureRegistry[F](
      client = client,
      namespace = namespace,
      monad = new CatsMonadError[F](blocker)
    )

object RedissonCatsFeatureRegistry {
  def useClient[F[_]: ContextShift: Sync](
    client: RedissonClient,
    namespace: String,
    blocker: Blocker
  ): RedissonCatsFeatureRegistry[F] = new RedissonCatsFeatureRegistry(client, namespace, blocker)
}
