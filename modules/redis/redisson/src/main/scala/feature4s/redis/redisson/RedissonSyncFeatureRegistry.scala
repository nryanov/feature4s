package feature4s.redis.redisson

import feature4s.Id
import feature4s.monad.IdMonadError
import org.redisson.api.RedissonClient

class RedissonSyncFeatureRegistry private (client: RedissonClient, namespace: String)
    extends RedissonFeatureRegistry[Id](
      client = client,
      namespace = namespace,
      monad = IdMonadError
    )

object RedissonSyncFeatureRegistry {
  def apply(client: RedissonClient, namespace: String): RedissonSyncFeatureRegistry =
    new RedissonSyncFeatureRegistry(client, namespace)
}
