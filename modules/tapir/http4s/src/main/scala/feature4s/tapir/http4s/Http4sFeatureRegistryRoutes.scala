package feature4s.tapir.http4s

import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.applicativeError._
import cats.syntax.semigroupk._
import sttp.model.StatusCode
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import feature4s.{ClientError, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.tapir.{BaseRoutes, Configuration, FeatureRegistryError}
import org.http4s.HttpRoutes
import org.http4s.implicits._
import sttp.tapir._
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.server.http4s.Http4sServerInterpreter

final class Http4sFeatureRegistryRoutes[F[_]: ContextShift: Timer](
  featureRegistry: FeatureRegistry[F],
  configuration: Configuration
)(implicit
  F: ConcurrentEffect[F],
  errorJsonCodec: JsonCodec[FeatureRegistryError],
  featureCodec: JsonCodec[FeatureState],
  featureListCodec: JsonCodec[List[FeatureState]]
) extends BaseRoutes {

  private val featureListRoute: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(featureListEndpoint)(_ =>
      toRoute(featureRegistry.featureList())
    )

  private val enableFeatureRoute: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(enableFeatureEndpoint)(featureName =>
      toRoute(featureRegistry.update(featureName, enable = true).map(_ => StatusCode.Ok))
    )

  private val disableFeatureRoute: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(disableFeatureEndpoint)(featureName =>
      toRoute(featureRegistry.update(featureName, enable = false).map(_ => StatusCode.Ok))
    )

  private val deleteFeatureRoute: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(deleteFeatureEndpoint)(featureName =>
      toRoute(
        featureRegistry
          .remove(featureName)
          .map(result => if (result) StatusCode.Ok else StatusCode.NotFound)
      )
    )

  private def toRoute[A](fa: F[A]): F[Either[(StatusCode, FeatureRegistryError), A]] =
    fa.map(_.asRight[(StatusCode, FeatureRegistryError)]).handleErrorWith {
      case err: FeatureNotFound => errorResponse(StatusCode.NotFound, err)
      case err: ClientError     => errorResponse(StatusCode.InternalServerError, err)
      case e: Throwable         => errorResponse(StatusCode.BadRequest, e)
    }

  private def errorResponse[A](
    code: StatusCode,
    reason: Throwable
  ): F[Either[(StatusCode, FeatureRegistryError), A]] =
    F.pure(
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

  val route = {
    if (configuration.allowRemove) {
      featureListRoute
        .combineK(enableFeatureRoute)
        .combineK(disableFeatureRoute)
        .combineK(deleteFeatureRoute)
    } else {
      featureListRoute.combineK(enableFeatureRoute).combineK(disableFeatureRoute)
    }
  }
}

object Http4sFeatureRegistryRoutes {
  def apply[F[_]: ContextShift: Timer](
    featureRegistry: FeatureRegistry[F],
    configuration: Configuration = Configuration.Default
  )(implicit
    F: ConcurrentEffect[F],
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): Http4sFeatureRegistryRoutes[F] =
    new Http4sFeatureRegistryRoutes[F](featureRegistry, configuration)
}
