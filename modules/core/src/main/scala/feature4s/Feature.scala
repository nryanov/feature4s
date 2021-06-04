package feature4s

final case class Feature[F[_], A](
  name: String,
  value: () => F[A],
  featureType: FeatureType[A],
  description: Option[String]
)
