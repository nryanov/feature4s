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

  def updateInfo(name: String, description: String): F[Unit]

  def featureList(): F[List[Feature[F]]]

  def isExist(name: String): F[Boolean]

  def remove(name: String): F[Boolean]

  def monadError: MonadError[F]
}
