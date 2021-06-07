package feature4s.redis.lettuce

import feature4s.{Feature, FeatureRegistry}
import feature4s.monad.MonadError
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

abstract class LettuceFeatureRegistry[F[_]](
  client: RedisClient,
  connection: StatefulRedisConnection[String, String],
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {
  private val syncCommands = connection.sync()

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    ???

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    ???

  override def update(name: String, enable: Boolean): F[Unit] = ???

  override def updateInfo(name: String, description: String): F[Unit] = ???

  override def featureList(): F[List[Feature[F]]] = ???

  override def isExist(name: String): F[Boolean] = ???

  override def remove(name: String): F[Boolean] = ???

  override def monadError: MonadError[F] = ???
}
