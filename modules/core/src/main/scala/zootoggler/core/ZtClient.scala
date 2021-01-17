package zootoggler.core

trait ZtClient[F[_]] {
  def register[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[F, A]]

  final def register[A: Converter](
    defaultValue: A,
    path: String
  ): F[FeatureAccessor[F, A]] = register(defaultValue, path, None)

  def remove(name: String): F[Boolean]

  def isExist(name: String): F[Boolean]

  def recreate[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[F, A]]

  final def recreate[A: Converter](
    defaultValue: A,
    name: String
  ): F[FeatureAccessor[F, A]] = recreate(defaultValue, name, None)

  def close(): F[Unit]
}
