package feature4s.tapir.akka

import akka.http.scaladsl.server.Route
import feature4s._
import feature4s.tapir.{BaseRoutes, Configuration, FeatureRegistryError}
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import akka.http.scaladsl.server.RouteConcatenation._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class AkkaFeatureRegistryRoutes(
  featureRegistry: FeatureRegistry[Future],
  configuration: Configuration
)(implicit
  ec: ExecutionContext,
  errorJsonCodec: JsonCodec[FeatureRegistryError],
  featureCodec: JsonCodec[FeatureState],
  featureListCodec: JsonCodec[List[FeatureState]]
) extends BaseRoutes {

  private val featureListRoute =
    AkkaHttpServerInterpreter.toRoute(featureListEndpoint)(_ => toRoute(featureRegistry.featureList()))

  private val enableFeatureRoute =
    AkkaHttpServerInterpreter.toRoute(enableFeatureEndpoint)(featureName =>
      toRoute(featureRegistry.update(featureName, enable = true).map(_ => StatusCode.Ok))
    )

  private val disableFeatureRoute =
    AkkaHttpServerInterpreter.toRoute(disableFeatureEndpoint)(featureName =>
      toRoute(featureRegistry.update(featureName, enable = false).map(_ => StatusCode.Ok))
    )

  private val deleteFeatureRoute =
    AkkaHttpServerInterpreter.toRoute(deleteFeatureEndpoint)(featureName =>
      toRoute(
        featureRegistry.remove(featureName).map(result => if (result) StatusCode.Ok else StatusCode.NotFound)
      )
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

  val endpoints = {
    if (configuration.allowRemove) {
      List(
        featureListEndpoint,
        enableFeatureEndpoint,
        disableFeatureEndpoint,
        deleteFeatureEndpoint
      )
    } else {
      List(
        featureListEndpoint,
        enableFeatureEndpoint,
        disableFeatureEndpoint
      )
    }
  }

  val route: Route = {
    if (configuration.allowRemove) {
      featureListRoute ~ enableFeatureRoute ~ disableFeatureRoute ~ deleteFeatureRoute
    } else {
      featureListRoute ~ enableFeatureRoute ~ disableFeatureRoute
    }
  }
}

object AkkaFeatureRegistryRoutes {
  def apply(
    featureRegistry: FeatureRegistry[Future],
    configuration: Configuration = Configuration.Default
  )(implicit
    ec: ExecutionContext,
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): AkkaFeatureRegistryRoutes = new AkkaFeatureRegistryRoutes(featureRegistry, configuration)
}
