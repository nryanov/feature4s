package feature4s.tapir.json.sprayjson

import feature4s.FeatureState
import feature4s.tapir.FeatureRegistryError
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.spray.TapirJsonSpray

object codecs extends DefaultJsonProtocol with TapirJsonSpray with SchemaDerivation {
  implicit val featureStateFormat: RootJsonFormat[FeatureState] = jsonFormat3(FeatureState)

  implicit val featureRegistryErrorFormat: RootJsonFormat[FeatureRegistryError] = jsonFormat2(
    FeatureRegistryError
  )
}
