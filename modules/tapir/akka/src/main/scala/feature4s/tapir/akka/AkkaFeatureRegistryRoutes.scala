package feature4s.tapir.akka

import feature4s.{FeatureRegistry, FeatureState}
import feature4s.tapir.{FeatureRegistryError, UpdateFeatureRequest}
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import akka.http.scaladsl.server.RouteConcatenation._
import sttp.tapir.{Endpoint, anyJsonBody, endpoint, path, plainBody, statusCode}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class AkkaFeatureRegistryRoutes(featureRegistry: FeatureRegistry[Future])(implicit
  ec: ExecutionContext,
  errorJsonCodec: JsonCodec[FeatureRegistryError],
  featureCodec: JsonCodec[FeatureState],
  featureListCodec: JsonCodec[List[FeatureState]]
) {
  private val baseEndpoint: Endpoint[Unit, (StatusCode, FeatureRegistryError), Unit, Any] =
    endpoint.in("features").errorOut(statusCode.and(anyJsonBody[FeatureRegistryError]))

  private val featureListEndpoint
    : Endpoint[Unit, (StatusCode, FeatureRegistryError), List[FeatureState], Any] =
    baseEndpoint.get.out(anyJsonBody[List[FeatureState]]).description("Get registered feature list")

  private val updateFeatureEndpoint
    : Endpoint[UpdateFeatureRequest, (StatusCode, FeatureRegistryError), StatusCode, Any] =
    baseEndpoint.put
      .in(path[String]("featureName"))
      .in(plainBody[Boolean].example(true))
      .mapInTo(UpdateFeatureRequest)
      .out(statusCode.example(StatusCode.Ok))
      .description("Update feature value")

  private val featureListRoute =
    AkkaHttpServerInterpreter.toRoute(featureListEndpoint)(_ =>
      toRoute(featureRegistry.featureList())
    )

  private val updateFeatureRoute =
    AkkaHttpServerInterpreter.toRoute(updateFeatureEndpoint)(request =>
      toRoute(featureRegistry.update(request.featureName, request.enable).map(_ => StatusCode.Ok))
    )

  private def toRoute[A](fa: Future[A]): Future[Either[(StatusCode, FeatureRegistryError), A]] =
    fa.transformWith {
      case Failure(exception) =>
        Future.successful(
          Left(
            StatusCode.BadRequest,
            FeatureRegistryError(
              code = StatusCode.BadRequest.code,
              reason = exception.getLocalizedMessage
            )
          )
        )
      case Success(value) => Future.successful(Right(value))
    }

  val endpoints = List(
    featureListEndpoint,
    updateFeatureEndpoint
  )

  val route = featureListRoute ~ updateFeatureRoute
}

object AkkaFeatureRegistryRoutes {
  def apply(featureRegistry: FeatureRegistry[Future])(implicit
    ec: ExecutionContext,
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): AkkaFeatureRegistryRoutes = new AkkaFeatureRegistryRoutes(featureRegistry)
}
