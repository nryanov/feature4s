package feature4s.tapir.json.tethys

import feature4s.FeatureState
import feature4s.tapir.FeatureRegistryError
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.TapirJsonTethys
import tethys.{JsonReader, JsonWriter}

object codecs extends TapirJsonTethys with SchemaDerivation {
  implicit val featureStateJsonReader: JsonReader[FeatureState] =
    JsonReader.builder
      .addField[String]("name")
      .addField[Boolean]("isEnable")
      .addField[Option[String]]("description")
      .buildReader(FeatureState.apply)

  implicit val featureStateJsonWriter: JsonWriter[FeatureState] =
    JsonWriter
      .obj[FeatureState]
      .addField[String]("name")(_.name)
      .addField[Boolean]("isEnable")(_.isEnable)
      .addField[Option[String]]("description")(_.description)

  implicit val featureRegistryErrorReader: JsonReader[FeatureRegistryError] =
    JsonReader.builder.addField[Int]("code").addField[String]("reason").buildReader(FeatureRegistryError.apply)

  implicit val featureRegistryErrorWriter: JsonWriter[FeatureRegistryError] =
    JsonWriter.obj[FeatureRegistryError].addField[Int]("code")(_.code).addField[String]("reason")(_.reason)
}
