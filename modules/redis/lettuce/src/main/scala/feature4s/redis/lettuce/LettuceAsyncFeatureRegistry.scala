package feature4s.redis.lettuce

import feature4s.redis._
import feature4s.monad.syntax._
import feature4s.monad.MonadAsyncError
import feature4s.{ClientError, Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import io.lettuce.core.{KeyScanCursor, KeyValue, ScanArgs}
import io.lettuce.core.api.StatefulRedisConnection

import scala.jdk.CollectionConverters._

abstract class LettuceAsyncFeatureRegistry[F[_]](
  connection: StatefulRedisConnection[String, String],
  namespace: String,
  implicit val monad: MonadAsyncError[F]
) extends FeatureRegistry[F] {
  private val asyncCommands = connection.async()

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .cancelable[Boolean] { cb =>
        val cf = asyncCommands
          .hsetnx(
            key(name, namespace),
            ValueFieldName,
            enable.toString
          )
          .whenComplete { (r: java.lang.Boolean, err: Throwable) =>
            if (err != null) cb(Left(ClientError(err)))
            else cb(Right(r))
          }

        () => monad.eval(cf.toCompletableFuture.cancel(true))
      }
      .flatMap(_ => updateInfo(name, description.getOrElse("")))
      .map(_ => Feature(name, () => valueAccessor(name), description))

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    monad
      .cancelable[Unit] { cb =>
        val cf = asyncCommands
          .hmset(
            key(name, namespace),
            Map(
              FeatureNameFieldName -> name,
              ValueFieldName -> enable.toString,
              DescriptionFieldName -> description.getOrElse("")
            ).asJava
          )
          .whenComplete { (_: java.lang.String, err: Throwable) =>
            if (err != null) cb(Left(ClientError(err)))
            else cb(Right(()))
          }

        () => monad.eval(cf.toCompletableFuture.cancel(true))
      }
      .map(_ => Feature(name, () => valueAccessor(name), description))

  private def valueAccessor(name: String): F[Boolean] =
    monad
      .cancelable[String] { cb =>
        val cf = asyncCommands.hget(key(name, namespace), ValueFieldName).whenComplete {
          (r: java.lang.String, err: Throwable) =>
            if (err != null) cb(Left(ClientError(err)))
            else cb(Right(r))
        }

        () => monad.eval(cf.toCompletableFuture.cancel(true))
      }
      .flatMap { value =>
        if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(name))
        else monad.eval(value.toBoolean)
      }

  private def updateInfo(name: String, description: String): F[Unit] =
    monad
      .cancelable[Long] { cb =>
        val cf = asyncCommands
          .hset(
            key(name, namespace),
            Map(
              FeatureNameFieldName -> name,
              DescriptionFieldName -> description
            ).asJava
          )
          .whenComplete { (r: java.lang.Long, err: Throwable) =>
            if (err != null) cb(Left(ClientError(err)))
            else cb(Right(r))
          }

        () => monad.eval(cf.toCompletableFuture.cancel(true))
      }
      .void

  override def update(name: String, enable: Boolean): F[Unit] =
    monad.ifM(isExist(name))(
      ifTrue = monad
        .cancelable[Boolean] { cb =>
          val cf =
            asyncCommands.hset(key(name, namespace), ValueFieldName, enable.toString).whenComplete {
              (r: java.lang.Boolean, err: Throwable) =>
                if (err != null) cb(Left(ClientError(err)))
                else cb(Right(r))
            }

          () => monad.eval(cf.toCompletableFuture.cancel(true))
        }
        .void,
      ifFalse = monad.raiseError(FeatureNotFound(name))
    )

  override def featureList(): F[List[FeatureState]] = {
    val filter = ScanArgs.Builder.matches(keyFilter(namespace)).limit(ScanLimit)

    def scan(cursor: KeyScanCursor[String], keys: List[String]): F[List[String]] =
      monad.ifM(monad.pure(cursor.isFinished))(
        monad.pure(keys),
        monad
          .cancelable[KeyScanCursor[String]] { cb =>
            val cf = asyncCommands.scan(cursor, filter).whenComplete {
              (cursor: KeyScanCursor[String], err: Throwable) =>
                if (err != null) cb(Left(ClientError(err)))
                else cb(Right(cursor))
            }

            () => monad.eval(cf.toCompletableFuture.cancel(true))
          }
          .flatMap(cursor => scan(cursor, cursor.getKeys.asScala.toList ::: keys))
      )

    monad
      .cancelable[KeyScanCursor[String]] { cb =>
        val cf = asyncCommands.scan(filter).whenComplete {
          (cursor: KeyScanCursor[String], err: Throwable) =>
            if (err != null) cb(Left(ClientError(err)))
            else cb(Right(cursor))
        }
        () => monad.eval(cf.toCompletableFuture.cancel(true))
      }
      .flatMap(cursor => scan(cursor, cursor.getKeys.asScala.toList))
      .flatMap { keys =>
        monad.traverse(keys)(key =>
          monad
            .cancelable[java.util.List[KeyValue[String, String]]] { cb =>
              val cf =
                asyncCommands
                  .hmget(key, FeatureNameFieldName, ValueFieldName, DescriptionFieldName)
                  .whenComplete { (list, err) =>
                    if (err != null) cb(Left(ClientError(err)))
                    else cb(Right(list))
                  }

              () => monad.eval(cf.toCompletableFuture.cancel(true))
            }
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

  override def isExist(name: String): F[Boolean] = monad.cancelable[Boolean] { cb =>
    val cf = asyncCommands.exists(key(name, namespace)).whenComplete {
      (r: java.lang.Long, err: Throwable) =>
        if (err != null) cb(Left(ClientError(err)))
        else cb(Right(r > 0))
    }

    () => monad.eval(cf.toCompletableFuture.cancel(true))
  }

  override def remove(name: String): F[Boolean] =
    monad.cancelable[Boolean] { cb =>
      val cf = asyncCommands.unlink(key(name, namespace)).whenComplete {
        (r: java.lang.Long, err: Throwable) =>
          if (err != null) cb(Left(ClientError(err)))
          else cb(Right(r > 0))
      }

      () => monad.eval(cf.toCompletableFuture.cancel(true))
    }

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadAsyncError[F] = monad
}
