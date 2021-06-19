package feature4s

package object redis {
  val DefaultNamespace: String = "features"
  val FeatureNameFieldName: String = "name"
  val ValueFieldName: String = "value"
  val DescriptionFieldName: String = "description"
  val ScanLimit: Long = 1000L

  def key(featureName: String, namespace: String): String =
    s"$namespace:feature:$featureName"

  def keyFilter(namespace: String): String =
    s"$namespace:feature:*"
}
