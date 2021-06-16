package feature4s

import feature4s.monad.MonadError

trait FeatureRegistry[F[_]] {
  def register(
    name: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]]

  def recreate(
    name: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]]

  def update(name: String, enable: Boolean): F[Unit]

  def featureList(): F[List[FeatureState]]

  def isExist(name: String): F[Boolean]

  def remove(name: String): F[Boolean]

  /**
   * Close only internal resources.
   * Resources such as connections, clients, etc should be close on client side code
   */
  def close(): F[Unit]

  def monadError: MonadError[F]
}
