package feature4s.tapir

import feature4s.FeatureState
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.{Endpoint, anyJsonBody, endpoint, path, statusCode}

abstract class BaseRoutes(implicit
  errorJsonCodec: JsonCodec[FeatureRegistryError],
  featureCodec: JsonCodec[FeatureState],
  featureListCodec: JsonCodec[List[FeatureState]]
) {

  private val baseEndpoint: Endpoint[Unit, (StatusCode, FeatureRegistryError), Unit, Any] =
    endpoint.in("features").errorOut(statusCode.and(anyJsonBody[FeatureRegistryError]))

  private[tapir] val featureListEndpoint
    : Endpoint[Unit, (StatusCode, FeatureRegistryError), List[FeatureState], Any] =
    baseEndpoint.get
      .out(anyJsonBody[List[FeatureState]])
      .description("Get registered feature list")
      .tag("features")

  private[tapir] val enableFeatureEndpoint
    : Endpoint[String, (StatusCode, FeatureRegistryError), StatusCode, Any] =
    baseEndpoint.put
      .in(path[String]("featureName"))
      .in("enable")
      .out(statusCode.example(StatusCode.Ok))
      .description("Enable feature")
      .tag("features")

  private[tapir] val disableFeatureEndpoint
    : Endpoint[String, (StatusCode, FeatureRegistryError), StatusCode, Any] =
    baseEndpoint.put
      .in(path[String]("featureName"))
      .in("disable")
      .out(statusCode.example(StatusCode.Ok))
      .description("Disable feature")
      .tag("features")

  private[tapir] val deleteFeatureEndpoint
    : Endpoint[String, (StatusCode, FeatureRegistryError), StatusCode, Any] =
    baseEndpoint.delete
      .in(path[String]("featureName"))
      .out(statusCode.example(StatusCode.Ok))
      .description("Delete feature")
      .tag("features")
}
