package feature4s

import feature4s.monad.MonadError

trait FeatureRegistry[F[_]] {
  def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]]

  def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]]

  def update(featureName: String, enable: Boolean): F[Unit]

  def featureList(): F[List[FeatureState]]

  def isExist(featureName: String): F[Boolean]

  def remove(featureName: String): F[Boolean]

  /**
   * Close only internal resources.
   * Resources such as connections, clients, etc should be closed on client side code
   */
  def close(): F[Unit]

  def monadError: MonadError[F]
}
