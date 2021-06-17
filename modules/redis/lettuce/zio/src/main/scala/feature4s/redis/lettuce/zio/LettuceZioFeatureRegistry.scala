package feature4s.redis.lettuce.zio

import feature4s.effect.zio.ZioMonadError
import feature4s.redis.lettuce.LettuceFeatureRegistry
import io.lettuce.core.api.StatefulRedisConnection
import zio.Task
import zio.blocking.Blocking

class LettuceZioFeatureRegistry private (
  connection: StatefulRedisConnection[String, String],
  namespace: String,
  blocking: Blocking.Service
) extends LettuceFeatureRegistry[Task](connection, namespace, new ZioMonadError(blocking))

object LettuceZioFeatureRegistry {
  def useConnection(
    connection: StatefulRedisConnection[String, String],
    namespace: String,
    blocking: Blocking.Service
  ): LettuceZioFeatureRegistry = new LettuceZioFeatureRegistry(connection, namespace, blocking)
}
