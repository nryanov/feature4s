package feature4s.redis.jedis

import feature4s.redis._
import feature4s.monad.syntax._
import feature4s.monad.MonadError
import redis.clients.jedis.{JedisCluster, ScanParams, ScanResult}
import feature4s.compat.CollectionConverters._
import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState, MissingFields}

abstract class JedisClusterFeatureRegistry[F[_]](
  jedisCluster: JedisCluster,
  namespace: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {
  override def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .eval(
        jedisCluster.hsetnx(
          key(featureName, namespace),
          ValueFieldName,
          enable.toString
        )
      )
      .flatMap(_ => updateInfo(featureName, description.getOrElse("")))
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  override def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .eval(
        jedisCluster.hmset(
          key(featureName, namespace),
          Map(
            FeatureNameFieldName -> featureName,
            ValueFieldName -> enable.toString,
            DescriptionFieldName -> description.getOrElse("")
          )
        )
      )
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  private def valueAccessor(featureName: String): F[Boolean] =
    monad.eval(jedisCluster.hget(key(featureName, namespace), ValueFieldName)).flatMap { value =>
      if (value == null || value.isEmpty) monad.raiseError(FeatureNotFound(featureName))
      else monad.eval(value.toBoolean)
    }

  private def updateInfo(featureName: String, description: String): F[Unit] =
    monad
      .eval(
        jedisCluster.hset(
          key(featureName, namespace),
          Map(
            FeatureNameFieldName -> featureName,
            DescriptionFieldName -> description
          )
        )
      )
      .void

  override def update(featureName: String, enable: Boolean): F[Unit] =
    monad.ifM(monad.eval(jedisCluster.exists(key(featureName, namespace))))(
      ifTrue = monad.eval(jedisCluster.hset(key(featureName, namespace), ValueFieldName, enable.toString)),
      ifFalse = monad.raiseError(FeatureNotFound(featureName))
    )

  override def featureList(): F[List[FeatureState]] = {
    val startCursor = ScanParams.SCAN_POINTER_START
    val filter = new ScanParams().`match`(keyFilter(namespace))

    def scan(cursor: ScanResult[String], keys: List[String]): F[List[String]] =
      monad.ifM(monad.pure(cursor.isCompleteIteration))(
        monad.pure(keys),
        monad
          .eval(jedisCluster.scan(cursor.getCursor, filter))
          .flatMap(cursor => scan(cursor, cursor.getResult ::: keys))
      )

    monad.eval(jedisCluster.scan(startCursor, filter)).flatMap(cursor => scan(cursor, cursor.getResult)).flatMap {
      keys =>
        monad.traverse(keys)(key =>
          monad
            .eval(
              jedisCluster.hmget(key, FeatureNameFieldName, ValueFieldName, DescriptionFieldName)
            )
            .map(fields => fields.toList)
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

  override def isExist(featureName: String): F[Boolean] =
    monad.eval(jedisCluster.exists(key(featureName, namespace)))

  override def remove(featureName: String): F[Boolean] =
    monad.eval(jedisCluster.unlink(key(featureName, namespace))).map(_ > 0)

  override def close(): F[Unit] = monad.unit

  override def monadError: MonadError[F] = monad
}
