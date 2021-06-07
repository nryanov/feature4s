package feature4s

final case class Feature[F[_]](
  name: String,
  isEnable: () => F[Boolean],
  description: Option[String]
)
