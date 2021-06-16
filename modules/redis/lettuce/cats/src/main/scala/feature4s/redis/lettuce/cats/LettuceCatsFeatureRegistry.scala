package feature4s.redis.lettuce.cats

import cats.effect.{Blocker, ContextShift, Sync}
import feature4s.effect.cats.CatsMonadError
import feature4s.redis.lettuce.LettuceFeatureRegistry
import io.lettuce.core.api.StatefulRedisConnection

class LettuceCatsFeatureRegistry[F[_]: ContextShift: Sync] private (
  connection: StatefulRedisConnection[String, String],
  namespace: String,
  blocker: Blocker
) extends LettuceFeatureRegistry[F](
      connection = connection,
      namespace = namespace,
      monad = new CatsMonadError[F](blocker)
    )

object LettuceCatsFeatureRegistry {
  def useConnection[F[_]: ContextShift: Sync](
    connection: StatefulRedisConnection[String, String],
    namespace: String,
    blocker: Blocker
  ): LettuceCatsFeatureRegistry[F] = new LettuceCatsFeatureRegistry(connection, namespace, blocker)
}
