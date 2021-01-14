package zootoggler.core

final case class Feature[A](
  value: A,
  name: String,
  description: Option[String]
) {
  override def toString: String = s"Feature: $name ${description.getOrElse("")}"
}

object Feature {
  def apply[A](
    value: A,
    name: String,
    description: Option[String]
  ): Feature[A] = new Feature(value, name, description)

  def apply[A](value: A, name: String): Feature[A] =
    new Feature(value, name, None)
}
