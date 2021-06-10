package feature4s.redis.lettuce

import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.monad.syntax._
import feature4s.monad.MonadError
import feature4s.redis._
import io.lettuce.core.{KeyScanCursor, ScanArgs}
import io.lettuce.core.api.StatefulRedisConnection

import scala.jdk.CollectionConverters._

abstract class LettuceFeatureRegistry[F[_]](
  connection: StatefulRedisConnection[String, String],
  namespace: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {
  private val syncCommands = connection.sync()

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .eval(
        syncCommands.hsetnx(
          key(name, namespace),
          ValueFieldName,
          enable.toString
        )
      )
      .flatMap(_ => updateInfo(name, description.getOrElse("")))
      .map(_ => Feature(name, () => valueAccessor(name), description))

  private def valueAccessor(name: String): F[Boolean] =
    monad.eval(syncCommands.hget(key(name, namespace), ValueFieldName)).flatMap { value =>
      if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(name))
      else monad.eval(value.toBoolean)
    }

  private def updateInfo(name: String, description: String): F[Unit] =
    monad
      .eval(
        syncCommands.hset(
          key(name, namespace),
          Map(
            FeatureNameFieldName -> name,
            DescriptionFieldName -> description
          ).asJava
        )
      )
      .void

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .eval(
        syncCommands.hmset(
          key(name, namespace),
          Map(
            FeatureNameFieldName -> name,
            ValueFieldName -> enable.toString,
            DescriptionFieldName -> description.getOrElse("")
          ).asJava
        )
      )
      .map(_ => Feature(name, () => valueAccessor(name), description))

  override def update(name: String, enable: Boolean): F[Unit] =
    monad.ifM(isExist(name))(
      ifTrue =
        monad.eval(syncCommands.hset(key(name, namespace), ValueFieldName, enable.toString)).void,
      ifFalse = monad.raiseError(FeatureNotFound(name))
    )

  override def featureList(): F[List[FeatureState]] = {
    val filter = ScanArgs.Builder.matches(keyFilter(namespace)).limit(ScanLimit)

    def scan(cursor: KeyScanCursor[String], keys: List[String]): F[List[String]] =
      monad.ifM(monad.pure(cursor.isFinished))(
        monad.pure(keys),
        monad
          .eval(syncCommands.scan(cursor, filter))
          .flatMap(cursor => scan(cursor, cursor.getKeys.asScala.toList ::: keys))
      )

    monad
      .eval(syncCommands.scan(filter))
      .flatMap(cursor => scan(cursor, cursor.getKeys.asScala.toList))
      .flatMap { keys =>
        monad.traverse(keys)(key =>
          monad
            .eval(
              syncCommands.hmget(key, FeatureNameFieldName, ValueFieldName, DescriptionFieldName)
            )
            .map(fields => fields.asScala.map(f => f.getKey -> f.getValue).toMap)
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

  override def isExist(name: String): F[Boolean] =
    monad.eval(syncCommands.exists(key(name, namespace))).map(_ > 0)

  override def remove(name: String): F[Boolean] =
    monad.eval(syncCommands.unlink(key(name, namespace))).map(_ > 0)

  override def close(): F[Unit] = monad.eval(connection.close())

  override def monadError: MonadError[F] = monad
}
