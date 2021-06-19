package feature4s.redis.redisson

import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.monad.MonadError
import feature4s.monad.syntax._
import feature4s.compat.CollectionConverters._
import feature4s.redis._
import org.redisson.api.{RKeys, RedissonClient}
import org.redisson.client.codec.StringCodec

abstract class RedissonFeatureRegistry[F[_]](
  client: RedissonClient,
  namespace: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {
  private val keyCommands: RKeys = client.getKeys
  private val codec: StringCodec = StringCodec.INSTANCE

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .eval(
        client
          .getMap[String, String](key(name, namespace), codec)
          .putIfAbsent(ValueFieldName, enable.toString)
      )
      .flatMap(_ => updateInfo(name, description.getOrElse("")))
      .map(_ => Feature(name, () => valueAccessor(name), description))

  private def valueAccessor(name: String): F[Boolean] =
    monad
      .eval(
        client.getMap[String, String](key(name, namespace), codec).get(ValueFieldName)
      )
      .flatMap { value =>
        if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(name))
        else monad.eval(value.toBoolean)
      }

  private def updateInfo(name: String, description: String): F[Unit] =
    monad
      .eval(
        client
          .getMap[String, String](key(name, namespace), codec)
          .putAll(
            Map(
              FeatureNameFieldName -> name,
              DescriptionFieldName -> description
            )
          )
      )
      .void

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .eval(
        client
          .getMap[String, String](key(name, namespace), codec)
          .putAll(
            Map(
              ValueFieldName -> enable.toString,
              FeatureNameFieldName -> name,
              DescriptionFieldName -> description.getOrElse("")
            )
          )
      )
      .map(_ => Feature(name, () => valueAccessor(name), description))

  override def update(name: String, enable: Boolean): F[Unit] =
    monad.ifM(isExist(name))(
      ifTrue = monad
        .eval(
          client
            .getMap[String, String](key(name, namespace), codec)
            .put(ValueFieldName, enable.toString)
        )
        .void,
      ifFalse = monad.raiseError(FeatureNotFound(name))
    )

  override def featureList(): F[List[FeatureState]] =
    monad
      .eval(keyCommands.getKeysByPattern(keyFilter(namespace)))
      .flatMap(iter => monad.eval(iter.toList))
      .flatMap { keys =>
        monad.traverse(keys)(key =>
          monad
            .eval[Map[String, String]](
              client
                .getMap[String, String](key, codec)
                .getAll(Set(FeatureNameFieldName, ValueFieldName, DescriptionFieldName))
            )
            .map(fields =>
              FeatureState(
                fields.getOrElse(FeatureNameFieldName, "empty_feature_name"),
                fields.get(ValueFieldName).exists(_.toBoolean),
                fields.get(DescriptionFieldName).filter(_.nonEmpty)
              )
            )
        )
      }

  override def isExist(name: String): F[Boolean] =
    monad.eval(client.getMap(key(name, namespace)).isExists)

  override def remove(name: String): F[Boolean] =
    monad.eval(keyCommands.delete(key(name, namespace))).map(_ > 0)

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadError[F] = monad
}
