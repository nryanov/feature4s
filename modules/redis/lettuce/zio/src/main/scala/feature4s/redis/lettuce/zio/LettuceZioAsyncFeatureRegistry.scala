package feature4s.redis.lettuce.zio

import feature4s.effect.zio.ZioMonadAsyncError
import feature4s.redis.lettuce.LettuceAsyncFeatureRegistry
import io.lettuce.core.api.StatefulRedisConnection
import zio.Task

class LettuceZioAsyncFeatureRegistry private (
  connection: StatefulRedisConnection[String, String],
  namespace: String
) extends LettuceAsyncFeatureRegistry[Task](connection, namespace, new ZioMonadAsyncError()) {}

object LettuceZioAsyncFeatureRegistry {
  def useClient(
    connection: StatefulRedisConnection[String, String],
    namespace: String
  ): LettuceZioAsyncFeatureRegistry = new LettuceZioAsyncFeatureRegistry(connection, namespace)
}
