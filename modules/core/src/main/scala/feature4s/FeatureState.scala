package feature4s

/** current feature state (for api responses) */
final case class FeatureState(
  name: String,
  value: String,
  featureType: String,
  description: Option[String]
)
