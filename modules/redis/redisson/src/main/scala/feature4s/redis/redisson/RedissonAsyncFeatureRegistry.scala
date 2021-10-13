package feature4s.redis.redisson

import feature4s.{ClientError, Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.monad.{MonadAsyncError, MonadError}
import feature4s.compat.CollectionConverters._
import feature4s.monad.syntax._
import feature4s.redis.{DescriptionFieldName, FeatureNameFieldName, ValueFieldName, key, keyFilter}
import org.redisson.api.{RKeys, RedissonClient}
import org.redisson.client.codec.StringCodec

abstract class RedissonAsyncFeatureRegistry[F[_]](
  client: RedissonClient,
  namespace: String,
  implicit val monad: MonadAsyncError[F]
) extends FeatureRegistry[F] {
  private val keyCommands: RKeys = client.getKeys
  private val codec: StringCodec = StringCodec.INSTANCE

  override def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .cancelable[Unit] { cb =>
        val cf = client
          .getMap[String, String](key(featureName, namespace), codec)
          .putIfAbsentAsync(ValueFieldName, enable.toString)

        cf.onComplete { (_, err) =>
          if (err != null) cb(Left(ClientError(err)))
          else cb(Right(()))
        }

        () => monad.eval(cf.cancel(true))
      }
      .flatMap(_ => updateInfo(featureName, description.getOrElse("")))
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  private def valueAccessor(featureName: String): F[Boolean] =
    monad
      .cancelable[String] { cb =>
        val cf =
          client.getMap[String, String](key(featureName, namespace), codec).getAsync(ValueFieldName)

        cf.onComplete { (r, err) =>
          if (err != null) cb(Left(ClientError(err)))
          else cb(Right(r))
        }

        () => monad.eval(cf.cancel(true))
      }
      .flatMap { value =>
        if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(featureName))
        else monad.eval(value.toBoolean)
      }

  private def updateInfo(featureName: String, description: String): F[Unit] =
    monad.cancelable[Unit] { cb =>
      val cf = client
        .getMap[String, String](key(featureName, namespace), codec)
        .putAllAsync(
          Map(
            FeatureNameFieldName -> featureName,
            DescriptionFieldName -> description
          )
        )

      cf.onComplete { (_, err) =>
        if (err != null) cb(Left(ClientError(err)))
        else cb(Right(()))
      }

      () => monad.eval(cf.cancel(true))
    }

  override def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .cancelable[Unit] { cb =>
        val cf = client
          .getMap[String, String](key(featureName, namespace), codec)
          .putAllAsync(
            Map(
              ValueFieldName -> enable.toString,
              FeatureNameFieldName -> featureName,
              DescriptionFieldName -> description.getOrElse("")
            )
          )

        cf.onComplete { (_, err) =>
          if (err != null) cb(Left(ClientError(err)))
          else cb(Right(()))
        }

        () => monad.eval(cf.cancel(true))
      }
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  override def update(featureName: String, enable: Boolean): F[Unit] =
    monad.ifM(isExist(featureName))(
      ifTrue = monad.cancelable[Unit] { cb =>
        val cf =
          client.getMap[String, String](key(featureName, namespace), codec).putAsync(ValueFieldName, enable.toString)

        cf.onComplete { (r, err) =>
          if (err != null) cb(Left(ClientError(err)))
          else cb(Right(()))
        }

        () => monad.eval(cf.cancel(true))
      },
      ifFalse = monad.raiseError(FeatureNotFound(featureName))
    )

  override def featureList(): F[List[FeatureState]] =
    monad.eval(keyCommands.getKeysByPattern(keyFilter(namespace))).flatMap(iter => monad.eval(iter.toList)).flatMap {
      keys =>
        monad.traverse(keys)(key =>
          monad
            .cancelable[Map[String, String]] { cb =>
              val cf = client
                .getMap[String, String](key, codec)
                .getAllAsync(Set(FeatureNameFieldName, ValueFieldName, DescriptionFieldName))

              cf.onComplete { (map, err) =>
                if (err != null) cb(Left(ClientError(err)))
                else cb(Right(map))
              }

              () => monad.eval(cf.cancel(true))
            }
            .map(fields =>
              FeatureState(
                fields.getOrElse(FeatureNameFieldName, "empty_feature_name"),
                fields.get(ValueFieldName).exists(_.toBoolean),
                fields.get(DescriptionFieldName).filter(_.nonEmpty)
              )
            )
        )
    }

  override def isExist(featureName: String): F[Boolean] =
    monad.cancelable { cb =>
      val cf = client.getMap(key(featureName, namespace)).isExistsAsync

      cf.onComplete { (r, err) =>
        if (err != null) cb(Left(ClientError(err)))
        else cb(Right(r))
      }

      () => monad.eval(cf.cancel(true))
    }

  override def remove(featureName: String): F[Boolean] =
    monad.cancelable { cb =>
      val cf = keyCommands.deleteAsync(key(featureName, namespace))

      cf.onComplete { (r, err) =>
        if (err != null) cb(Left(ClientError(err)))
        else cb(Right(r > 0))
      }

      () => monad.eval(cf.cancel(true))
    }

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadError[F] = monad
}
