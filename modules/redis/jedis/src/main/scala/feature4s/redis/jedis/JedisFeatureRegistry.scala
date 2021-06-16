package feature4s.redis.jedis

import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState, MissingFields}
import feature4s.monad.MonadError
import feature4s.monad.syntax._
import feature4s.redis._
import redis.clients.jedis.{Jedis, JedisPool, ScanParams, ScanResult}

import scala.jdk.CollectionConverters._

abstract class JedisFeatureRegistry[F[_]](
  pool: JedisPool,
  namespace: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    useClient { client =>
      monad
        .eval(
          client.hsetnx(
            key(name, namespace),
            ValueFieldName,
            enable.toString
          )
        )
        .flatMap(_ => updateInfo(name, description.getOrElse("")))
        .map(_ => Feature(name, () => valueAccessor(name), description))
    }

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    useClient { client =>
      monad
        .eval(
          client.hmset(
            key(name, namespace),
            Map(
              FeatureNameFieldName -> name,
              ValueFieldName -> enable.toString,
              DescriptionFieldName -> description.getOrElse("")
            ).asJava
          )
        )
        .map(_ => Feature(name, () => valueAccessor(name), description))

    }

  private def valueAccessor(name: String): F[Boolean] = useClient { client =>
    monad.eval(client.hget(key(name, namespace), ValueFieldName)).flatMap { value =>
      if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(name))
      else monad.eval(value.toBoolean)
    }
  }

  private def updateInfo(name: String, description: String): F[Unit] = useClient { client =>
    monad
      .eval(
        client.hset(
          key(name, namespace),
          Map(
            FeatureNameFieldName -> name,
            DescriptionFieldName -> description
          ).asJava
        )
      )
      .void
  }

  override def update(name: String, enable: Boolean): F[Unit] = useClient { client =>
    monad.ifM(monad.eval(client.exists(key(name, namespace))))(
      ifTrue = monad.eval(client.hset(key(name, namespace), ValueFieldName, enable.toString)),
      ifFalse = monad.raiseError(FeatureNotFound(name))
    )
  }

  override def featureList(): F[List[FeatureState]] = useClient { client =>
    val startCursor = ScanParams.SCAN_POINTER_START
    val filter = new ScanParams().`match`(keyFilter(namespace))

    def scan(cursor: ScanResult[String], keys: List[String]): F[List[String]] =
      monad.ifM(monad.pure(cursor.isCompleteIteration))(
        monad.pure(keys),
        monad
          .eval(client.scan(cursor.getCursor, filter))
          .flatMap(cursor => scan(cursor, cursor.getResult.asScala.toList ::: keys))
      )

    monad
      .eval(client.scan(startCursor, filter))
      .flatMap(cursor => scan(cursor, cursor.getResult.asScala.toList))
      .flatMap { keys =>
        monad.traverse(keys)(key =>
          monad
            .eval(
              client.hmget(key, FeatureNameFieldName, ValueFieldName, DescriptionFieldName)
            )
            .map(fields => fields.asScala.toList)
            .flatMap {
              case featureName :: value :: description :: Nil =>
                monad.pure(
                  FeatureState(
                    Option(featureName).filter(_.nonEmpty).getOrElse("empty_feature_name"),
                    value.toBoolean,
                    Option(description).filter(_.nonEmpty)
                  )
                )
              case _ => monad.raiseError(MissingFields(key))
            }
        )
      }
  }

  override def isExist(name: String): F[Boolean] = useClient { client =>
    monad.eval(client.exists(key(name, namespace)))
  }

  override def remove(name: String): F[Boolean] = useClient { client =>
    monad.eval(client.unlink(key(name, namespace))).map(_ > 0)
  }

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadError[F] = monad

  private def useClient[A](fa: Jedis => F[A]): F[A] =
    monad.bracket(monad.eval(pool.getResource))(fa)(client => monad.eval(client.close()))
}
