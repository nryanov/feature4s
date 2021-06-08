package feature4s.redis.lettuce

import feature4s.Id
import feature4s.monad.IdMonadError
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

class LettuceSyncFeatureRegistry private (
  connection: StatefulRedisConnection[String, String],
  namespace: String
) extends LettuceFeatureRegistry[Id](
      connection = connection,
      namespace = namespace,
      monad = IdMonadError
    )

object LettuceSyncFeatureRegistry {
  def apply(
    client: RedisClient,
    namespace: String
  ): LettuceSyncFeatureRegistry =
    new LettuceSyncFeatureRegistry(
      connection = client.connect(),
      namespace = namespace
    )
}
