package zootoggler.core

trait ZtClient[F[_]] {
  def register[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[F, A]]

  final def register[A: FeatureType](
    defaultValue: A,
    name: String
  ): F[FeatureAccessor[F, A]] = register(defaultValue, name, None)

  def update[A: FeatureType](name: String, newValue: A): F[Boolean]

  def remove(name: String): F[Boolean]

  def isExist(name: String): F[Boolean]

  def recreate[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[F, A]]

  final def recreate[A: FeatureType](
    defaultValue: A,
    name: String
  ): F[FeatureAccessor[F, A]] = recreate(defaultValue, name, None)

  def close(): F[Unit]
}
