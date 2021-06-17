package feature4s.redis.redisson.cats

import cats.effect.Concurrent
import feature4s.effect.cats.CatsMonadAsyncError
import feature4s.redis.redisson.RedissonAsyncFeatureRegistry
import org.redisson.api.RedissonClient

class RedissonCatsAsyncFeatureRegistry[F[_]: Concurrent] private (
  client: RedissonClient,
  namespace: String
) extends RedissonAsyncFeatureRegistry[F](
      client = client,
      namespace = namespace,
      monad = new CatsMonadAsyncError[F]()
    )

object RedissonCatsAsyncFeatureRegistry {
  def useClient[F[_]: Concurrent](
    client: RedissonClient,
    namespace: String
  ): RedissonCatsAsyncFeatureRegistry[F] = new RedissonCatsAsyncFeatureRegistry(client, namespace)
}
