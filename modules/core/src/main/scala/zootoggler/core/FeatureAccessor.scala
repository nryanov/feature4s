package zootoggler.core

trait FeatureAccessor[F[_], A] {
  def value: F[A]

  def update(newValue: A): F[Boolean]
}
