package feature4s.redis.lettuce.cats

import cats.effect.Concurrent
import feature4s.effect.cats.CatsMonadAsyncError
import feature4s.redis.lettuce.LettuceAsyncFeatureRegistry
import io.lettuce.core.api.StatefulRedisConnection

class LettuceCatsAsyncFeatureRegistry[F[_]: Concurrent] private (
  connection: StatefulRedisConnection[String, String],
  namespace: String
) extends LettuceAsyncFeatureRegistry[F](
      connection = connection,
      namespace = namespace,
      monad = new CatsMonadAsyncError[F]
    )

object LettuceCatsAsyncFeatureRegistry {
  def useConnection[F[_]: Concurrent](
    connection: StatefulRedisConnection[String, String],
    namespace: String
  ): LettuceCatsAsyncFeatureRegistry[F] = new LettuceCatsAsyncFeatureRegistry(connection, namespace)
}
