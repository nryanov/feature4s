package feature4s.redis.redisson.zio

import feature4s.effect.zio.ZioMonadAsyncError
import feature4s.redis.redisson.RedissonAsyncFeatureRegistry
import org.redisson.api.RedissonClient
import zio.Task

class RedissonZioAsyncFeatureRegistry private (client: RedissonClient, namespace: String)
    extends RedissonAsyncFeatureRegistry[Task](
      client = client,
      namespace = namespace,
      monad = new ZioMonadAsyncError
    )

object RedissonZioAsyncFeatureRegistry {
  def useClient(client: RedissonClient, namespace: String): RedissonZioAsyncFeatureRegistry =
    new RedissonZioAsyncFeatureRegistry(client, namespace)
}
