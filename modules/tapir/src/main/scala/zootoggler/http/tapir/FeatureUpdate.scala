package zootoggler.http.tapir

sealed trait FeatureUpdate

object FeatureUpdate {
  final case class StringInput(
    featureName: String,
    description: Option[String],
    value: String
  ) extends FeatureUpdate

  final case class ByteArrayInput(
    featureName: String,
    description: Option[String],
    value: Array[Byte]
  ) extends FeatureUpdate
}
