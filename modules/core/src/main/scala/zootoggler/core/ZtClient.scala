package zootoggler.core

trait ZtClient[F[_]] {
  def register[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): F[FeatureAccessor[A]]

  final def register[A: Converter](
    defaultValue: A,
    path: String
  ): F[FeatureAccessor[A]] = register(defaultValue, path, None, None)

  trait FeatureAccessor[A] {
    def value: F[Feature[A]]

    def update(newValue: A): F[Unit]
  }
}
