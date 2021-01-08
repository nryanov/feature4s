package zootoggler.core

final case class Feature[A](
  value: A,
  path: String,
  ns: Option[String],
  ds: Option[String]
) {
  val namespace: String = ns.getOrElse("")

  val description: String = ds.getOrElse("")
}

object Feature {
  def apply[A](
    value: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): Feature[A] = new Feature(value, path, namespace, description)

  def apply[A](value: A, path: String): Feature[A] =
    new Feature(value, path, None, None)

  def apply[A](value: A, path: String, namespace: Option[String]): Feature[A] =
    new Feature(value, path, namespace, None)
}
