package feature4s

package object redis {
  val DefaultNamespace: String = "features"
  val ValueFieldName: String = "value"
  val DescriptionFieldName: String = "description"
  val ScanLimit: Int = 1000

  def key(featureName: String, namespace: String): String =
    s"$namespace:feature:$featureName"

  def keyFilter(namespace: String): String =
    s"$namespace:feature:*"
}
