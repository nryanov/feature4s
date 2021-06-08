package feature4s

final case class FeatureState(
  name: String,
  isEnable: Boolean,
  description: Option[String]
)
