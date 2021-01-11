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

  def close(): F[Unit]

  trait FeatureAccessor[A] {
    def value: F[A]

    def cachedValue: A

    def update(newValue: A): F[Unit]
  }
}
