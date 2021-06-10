package feature4s.redis.lettuce

import feature4s.monad.FutureMonadAsyncError
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

import scala.concurrent.{ExecutionContext, Future}

class LettuceFutureFeatureRegistry private (
  connection: StatefulRedisConnection[String, String],
  namespace: String
)(implicit ec: ExecutionContext)
    extends LettuceAsyncFeatureRegistry[Future](
      connection = connection,
      namespace = namespace,
      monad = new FutureMonadAsyncError()
    )

object LettuceFutureFeatureRegistry {
  def apply(
    client: RedisClient,
    namespace: String
  )(implicit ec: ExecutionContext): LettuceFutureFeatureRegistry = {
    val connection = client.connect()

    new LettuceFutureFeatureRegistry(
      connection = connection,
      namespace = namespace
    )(ec)
  }
}
