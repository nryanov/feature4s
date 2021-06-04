package feature4s

import feature4s.monad.MonadError

trait FeatureRegistry[F[_]] {
  def register[A: FeatureType](
    name: String,
    defaultValue: A,
    description: Option[String]
  ): F[Feature[F, A]]

  def recreate[A: FeatureType](
    name: String,
    value: A,
    description: Option[String]
  ): F[Feature[F, A]]

  def update(name: String, value: String): F[Unit]

  def updateInfo(name: String, description: String): F[Unit]

  def featureList(): F[List[Feature[F, _]]]

  def isExist(name: String): F[Boolean]

  def remove(name: String): F[Boolean]

  def monadError: MonadError[F]
}
