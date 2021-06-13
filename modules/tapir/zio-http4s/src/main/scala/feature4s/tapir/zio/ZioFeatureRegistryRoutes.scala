package feature4s.tapir.zio

import feature4s.{FeatureRegistry, FeatureState}
import feature4s.tapir.{FeatureRegistryError, UpdateFeatureRequest}
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.ztapir._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio._
import zio.interop.catz._

final class ZioFeatureRegistryRoutes(featureRegistry: FeatureRegistry[Task])(implicit
  errorJsonCodec: JsonCodec[FeatureRegistryError],
  featureCodec: JsonCodec[FeatureState],
  featureListCodec: JsonCodec[List[FeatureState]]
) {
  private val baseEndpoint: ZEndpoint[Unit, (StatusCode, FeatureRegistryError), Unit] =
    endpoint.in("features").errorOut(statusCode.and(anyJsonBody[FeatureRegistryError]))

  private val featureListEndpoint
    : ZEndpoint[Unit, (StatusCode, FeatureRegistryError), List[FeatureState]] =
    baseEndpoint.get.out(anyJsonBody[List[FeatureState]]).description("Get registered feature list")

  private val updateFeatureEndpoint
    : ZEndpoint[UpdateFeatureRequest, (StatusCode, FeatureRegistryError), StatusCode] =
    baseEndpoint.put
      .in(path[String]("featureName"))
      .in(plainBody[Boolean].example(true))
      .mapInTo(UpdateFeatureRequest)
      .out(statusCode.example(StatusCode.Ok))
      .description("Update feature value")

  private val featureListRoute =
    ZHttp4sServerInterpreter
      .from(featureListEndpoint)(_ => toRoute(featureRegistry.featureList()))
      .toRoutes

  private val updateFeatureRoute =
    ZHttp4sServerInterpreter
      .from(updateFeatureEndpoint)(request =>
        toRoute(featureRegistry.update(request.featureName, request.enable).as(StatusCode.Ok))
      )
      .toRoutes

  private def toRoute[A](fa: Task[A]): ZIO[Any, (StatusCode, FeatureRegistryError), A] =
    fa.mapError { case err: Throwable =>
      (
        StatusCode.BadRequest,
        FeatureRegistryError(code = StatusCode.BadRequest.code, reason = err.getLocalizedMessage)
      )
    }

  val endpoints = List(
    featureListEndpoint,
    updateFeatureEndpoint
  )

  val route = List(featureListRoute, updateFeatureRoute)
}

object ZioFeatureRegistryRoutes {
  def apply(featureRegistry: FeatureRegistry[Task])(implicit
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): ZioFeatureRegistryRoutes =
    new ZioFeatureRegistryRoutes(featureRegistry)
}
