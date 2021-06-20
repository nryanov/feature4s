package feature4s.tapir.akka

import akka.http.scaladsl.server.Route
import feature4s._
import feature4s.tapir.FeatureRegistryError
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

  private[tapir] val featureListEndpoint
    : Endpoint[Unit, (StatusCode, FeatureRegistryError), List[FeatureState], Any] =
    baseEndpoint.get.out(anyJsonBody[List[FeatureState]]).description("Get registered feature list")

  private[tapir] val enableFeatureEndpoint
    : Endpoint[String, (StatusCode, FeatureRegistryError), StatusCode, Any] =
    baseEndpoint.put
      .in(path[String]("featureName"))
      .in("enable")
      .out(statusCode.example(StatusCode.Ok))
      .description("Enable feature")

  private[tapir] val disableFeatureEndpoint
    : Endpoint[String, (StatusCode, FeatureRegistryError), StatusCode, Any] =
    baseEndpoint.put
      .in(path[String]("featureName"))
      .in("disable")
      .out(statusCode.example(StatusCode.Ok))
      .description("Disable feature")

  private val featureListRoute =
    AkkaHttpServerInterpreter.toRoute(featureListEndpoint)(_ =>
      toRoute(featureRegistry.featureList())
    )

  private val enableFeatureRoute =
    AkkaHttpServerInterpreter.toRoute(enableFeatureEndpoint)(featureName =>
      toRoute(featureRegistry.update(featureName, enable = true).map(_ => StatusCode.Ok))
    )

  private val disableFeatureRoute =
    AkkaHttpServerInterpreter.toRoute(disableFeatureEndpoint)(featureName =>
      toRoute(featureRegistry.update(featureName, enable = false).map(_ => StatusCode.Ok))
    )

  private def toRoute[A](fa: Future[A]): Future[Either[(StatusCode, FeatureRegistryError), A]] =
    fa.transformWith {
      case Failure(exception) =>
        exception match {
          case err: FeatureNotFound => errorResponse(StatusCode.NotFound, err)
          case err: ClientError     => errorResponse(StatusCode.InternalServerError, err)
          case err: Throwable       => errorResponse(StatusCode.BadRequest, err)
        }
      case Success(value) => Future.successful(Right(value))
    }

  private def errorResponse[A](
    code: StatusCode,
    reason: Throwable
  ): Future[Either[(StatusCode, FeatureRegistryError), A]] =
    Future.successful(
      Left(
        (
          code,
          FeatureRegistryError(
            code = code.code,
            reason = reason.getLocalizedMessage
          )
        )
      )
    )

  val endpoints = List(
    featureListEndpoint,
    enableFeatureEndpoint,
    disableFeatureEndpoint
  )

  val route: Route = featureListRoute ~ enableFeatureRoute ~ disableFeatureRoute
}

object AkkaFeatureRegistryRoutes {
  def apply(featureRegistry: FeatureRegistry[Future])(implicit
    ec: ExecutionContext,
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): AkkaFeatureRegistryRoutes = new AkkaFeatureRegistryRoutes(featureRegistry)
}
