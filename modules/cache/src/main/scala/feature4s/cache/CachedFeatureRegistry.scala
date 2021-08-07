package feature4s.cache

import java.util.concurrent.TimeUnit

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import feature4s.monad.MonadError
import feature4s.monad.syntax._
import feature4s.{Feature, FeatureRegistry, FeatureState}

import scala.concurrent.duration.Duration

class CachedFeatureRegistry[F[_]] private (
  delegate: FeatureRegistry[F],
  cache: Cache[String, java.lang.Boolean],
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {

  override def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    delegate
      .register(featureName, enable, description)
      .flatMap(feature =>
        feature
          .isEnable()
          .flatMap(flag => monad.eval(cache.put(featureName, flag)))
          .map(_ => feature.copy(isEnable = () => valueAccessor(featureName, feature.isEnable)))
      )

  override def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] =
    delegate
      .recreate(featureName, enable, description)
      .flatMap(feature =>
        monad
          .eval(cache.put(featureName, enable))
          .map(_ => feature.copy(isEnable = () => valueAccessor(featureName, feature.isEnable)))
      )

  protected def valueAccessor(featureName: String, internal: () => F[Boolean]): F[Boolean] =
    monad.eval(cache.getIfPresent(featureName)).flatMap { state =>
      if (state != null) {
        monad.pure(state)
      } else {
        internal().flatMap(flag => monad.eval(cache.put(featureName, flag)).map(_ => flag))
      }
    }

  override def update(featureName: String, enable: Boolean): F[Unit] =
    delegate
      .update(featureName, enable)
      .flatMap(_ =>
        monad.eval {
          val current = cache.getIfPresent(featureName)
          if (current != null) {
            cache.put(featureName, enable)
          }
        }
      )

  // featureList get from the underlying feature registry and update current cache
  override def featureList(): F[List[FeatureState]] =
    delegate.featureList().flatMap { features =>
      monad
        .eval(features.foreach(feature => cache.put(feature.name, feature.isEnable)))
        .map(_ => features)
    }

  override def isExist(featureName: String): F[Boolean] =
    monad.eval(cache.getIfPresent(featureName)).flatMap { result =>
      if (result == null) {
        delegate.isExist(featureName)
      } else {
        monad.pure(true)
      }

    }

  override def remove(featureName: String): F[Boolean] =
    delegate.remove(featureName).flatMap(r => monad.eval(cache.invalidate(featureName)).map(_ => r))

  override def close(): F[Unit] =
    monad.eval(cache.cleanUp()) *> delegate.close()

  override def monadError: MonadError[F] = monad
}

object CachedFeatureRegistry {

  def wrap[F[_]](
    delegate: FeatureRegistry[F],
    ttl: Duration
  ): F[CachedFeatureRegistry[F]] = wrap(delegate, ttl, None)

  def wrap[F[_]](
    delegate: FeatureRegistry[F],
    ttl: Duration,
    maxSize: Option[Long]
  ): F[CachedFeatureRegistry[F]] = {
    implicit val monad: MonadError[F] = delegate.monadError

    monad.eval {
      var cacheBuilder = Caffeine.newBuilder()

      cacheBuilder = maxSize.fold(cacheBuilder)(size => cacheBuilder.maximumSize(size))
      cacheBuilder = cacheBuilder.expireAfterWrite(ttl.toMillis, TimeUnit.MILLISECONDS)

      cacheBuilder.build[String, java.lang.Boolean]()
    }.flatMap { cache =>
      // fill-up initial state
      delegate
        .featureList()
        .map(_.foreach(feature => cache.put(feature.name, feature.isEnable)))
        .map(_ => new CachedFeatureRegistry(delegate, cache, monad))
    }
  }
}
