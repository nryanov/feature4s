package feature4s.redis.lettuce

import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.monad.syntax._
import feature4s.monad.MonadError
import feature4s.compat.CollectionConverters._
import feature4s.redis._
import io.lettuce.core.{KeyScanCursor, ScanArgs}
import io.lettuce.core.api.StatefulRedisConnection

abstract class LettuceFeatureRegistry[F[_]](
  connection: StatefulRedisConnection[String, String],
  namespace: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {
  private val syncCommands = connection.sync()

  override def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .eval(
        syncCommands.hsetnx(
          key(featureName, namespace),
          ValueFieldName,
          enable.toString
        )
      )
      .flatMap(_ => updateInfo(featureName, description.getOrElse("")))
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  private def valueAccessor(featureName: String): F[Boolean] =
    monad.eval(syncCommands.hget(key(featureName, namespace), ValueFieldName)).flatMap { value =>
      if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(featureName))
      else monad.eval(value.toBoolean)
    }

  private def updateInfo(featureName: String, description: String): F[Unit] =
    monad
      .eval(
        syncCommands.hset(
          key(featureName, namespace),
          Map(
            FeatureNameFieldName -> featureName,
            DescriptionFieldName -> description
          )
        )
      )
      .void

  override def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .eval(
        syncCommands.hmset(
          key(featureName, namespace),
          Map(
            FeatureNameFieldName -> featureName,
            ValueFieldName -> enable.toString,
            DescriptionFieldName -> description.getOrElse("")
          )
        )
      )
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  override def update(featureName: String, enable: Boolean): F[Unit] =
    monad.ifM(isExist(featureName))(
      ifTrue = monad.eval(syncCommands.hset(key(featureName, namespace), ValueFieldName, enable.toString)).void,
      ifFalse = monad.raiseError(FeatureNotFound(featureName))
    )

  override def featureList(): F[List[FeatureState]] = {
    val filter = ScanArgs.Builder.matches(keyFilter(namespace)).limit(ScanLimit)

    def scan(cursor: KeyScanCursor[String], keys: List[String]): F[List[String]] =
      monad.ifM(monad.pure(cursor.isFinished))(
        monad.pure(keys),
        monad.eval(syncCommands.scan(cursor, filter)).flatMap(cursor => scan(cursor, cursor.getKeys ::: keys))
      )

    monad.eval(syncCommands.scan(filter)).flatMap(cursor => scan(cursor, cursor.getKeys)).flatMap { keys =>
      monad.traverse(keys)(key =>
        monad
          .eval(
            syncCommands.hmget(key, FeatureNameFieldName, ValueFieldName, DescriptionFieldName)
          )
          .map(fields => fields.map(f => f.getKey -> f.getValue).toMap)
          .map(fields =>
            FeatureState(
              fields.getOrElse(FeatureNameFieldName, "empty_feature_name"),
              fields.get(ValueFieldName).exists(_.toBoolean),
              fields.get(DescriptionFieldName).filter(_.nonEmpty)
            )
          )
      )
    }
  }

  override def isExist(featureName: String): F[Boolean] =
    monad.eval(syncCommands.exists(key(featureName, namespace))).map(_ > 0)

  override def remove(featureName: String): F[Boolean] =
    monad.eval(syncCommands.unlink(key(featureName, namespace))).map(_ > 0)

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadError[F] = monad
}
