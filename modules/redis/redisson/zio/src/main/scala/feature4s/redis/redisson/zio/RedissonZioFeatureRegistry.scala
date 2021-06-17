package feature4s.redis.redisson.zio

import feature4s.effect.zio.ZioMonadError
import feature4s.redis.redisson.RedissonFeatureRegistry
import org.redisson.api.RedissonClient
import zio._
import zio.blocking.Blocking

class RedissonZioFeatureRegistry private (
  client: RedissonClient,
  namespace: String,
  blocking: Blocking.Service
) extends RedissonFeatureRegistry[Task](
      client = client,
      namespace = namespace,
      monad = new ZioMonadError(blocking)
    )

object RedissonZioFeatureRegistry {
  def useClient(
    client: RedissonClient,
    namespace: String,
    blocking: Blocking.Service
  ): RedissonZioFeatureRegistry = new RedissonZioFeatureRegistry(client, namespace, blocking)
}
