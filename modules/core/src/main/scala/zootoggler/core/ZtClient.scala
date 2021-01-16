package zootoggler.core

trait ZtClient[F[_]] {
  def register[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[A]]

  final def register[A: Converter](
    defaultValue: A,
    path: String
  ): F[FeatureAccessor[A]] = register(defaultValue, path, None)

  def remove(name: String): F[Boolean]

  def isExist(name: String): F[Boolean]

  def recreate[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[A]]

  def close(): F[Unit]

  trait FeatureAccessor[A] {
    def value: F[A]

    def update(newValue: A): F[Boolean]
  }
}
