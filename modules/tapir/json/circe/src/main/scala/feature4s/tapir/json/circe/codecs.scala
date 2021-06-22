package feature4s.tapir.json.circe

import feature4s.FeatureState
import io.circe._
import feature4s.tapir.FeatureRegistryError
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe._

object codecs extends TapirJsonCirce with SchemaDerivation {
  implicit val featureRegistryErrorEncoder: Encoder[FeatureRegistryError] =
    (a: FeatureRegistryError) =>
      Json.obj(
        ("code", Json.fromInt(a.code)),
        ("reason", Json.fromString(a.reason))
      )

  implicit val featureRegistryErrorDecoder: Decoder[FeatureRegistryError] =
    (c: HCursor) =>
      for {
        code <- c.downField("code").as[Int]
        reason <- c.downField("reason").as[String]
      } yield FeatureRegistryError(code, reason)

  implicit val featureStateEncoder: Encoder[FeatureState] = (a: FeatureState) =>
    Json.obj(
      ("name", Json.fromString(a.name)),
      ("isEnable", Json.fromBoolean(a.isEnable)),
      ("description", a.description.map(Json.fromString).getOrElse(Json.Null))
    )

  implicit val featureStateDecoder: Decoder[FeatureState] =
    (c: HCursor) =>
      for {
        name <- c.downField("name").as[String]
        isEnable <- c.downField("isEnable").as[Boolean]
        description <- c.downField("description").as[Option[String]]
      } yield FeatureState(name, isEnable, description)
}
