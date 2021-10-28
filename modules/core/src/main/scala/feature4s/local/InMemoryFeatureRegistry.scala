package feature4s.local

import java.util.{Map => JMap}
import java.util.concurrent.{ConcurrentHashMap => JConcurrentHashMap}

import feature4s.{Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.compat.CollectionConverters._
import feature4s.monad.MonadError
import feature4s.monad.syntax._

abstract class InMemoryFeatureRegistry[F[_]](me: MonadError[F]) extends FeatureRegistry[F] {
  private implicit val monad: MonadError[F] = me

  private val features: JMap[String, FeatureState] = new JConcurrentHashMap[String, FeatureState]()

  override def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .eval(
        features.putIfAbsent(
          featureName,
          FeatureState(featureName, enable, description)
        )
      )
      .flatMap(_ =>
        monad.eval(
          features.computeIfPresent(featureName, (_, state) => state.copy(description = description))
        )
      )
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  override def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    monad
      .eval(
        features.put(featureName, FeatureState(featureName, enable, description))
      )
      .map(_ => Feature(featureName, () => valueAccessor(featureName), description))

  private def valueAccessor(featureName: String): F[Boolean] =
    monad.eval(features.get(featureName)).flatMap { value =>
      if (value == null) monad.raiseError(FeatureNotFound(featureName))
      else monad.pure(value.isEnable)
    }

  override def update(featureName: String, enable: Boolean): F[Unit] =
    monad
      .eval(
        features.computeIfPresent(
          featureName,
          (_, state) => state.copy(isEnable = enable)
        )
      )
      .flatMap(state => if (state == null) monad.raiseError(FeatureNotFound(featureName)) else monad.unit)

  override def featureList(): F[List[FeatureState]] =
    monad.eval(javaCollectionToScala(features.values()).toList)

  override def isExist(featureName: String): F[Boolean] =
    monad.eval(features.containsKey(featureName))

  override def remove(featureName: String): F[Boolean] =
    monad.eval(features.remove(featureName)).map(_ != null)

  override def close(): F[Unit] = monad.eval(features.clear())

  override def monadError: MonadError[F] = monad
}
