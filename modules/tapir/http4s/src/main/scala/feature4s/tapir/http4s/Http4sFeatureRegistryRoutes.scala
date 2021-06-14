package feature4s.tapir.http4s

import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.applicativeError._
import cats.syntax.semigroupk._
import sttp.model.StatusCode
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import feature4s.{ClientError, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.tapir.{FeatureRegistryError, UpdateFeatureRequest}
import org.http4s.HttpRoutes
import org.http4s.implicits._
import sttp.tapir._
import sttp.tapir.Codec.{JsonCodec, boolean}
import sttp.tapir.server.http4s.Http4sServerInterpreter

final class Http4sFeatureRegistryRoutes[F[_]: ContextShift: Timer](
  featureRegistry: FeatureRegistry[F]
)(implicit
  F: ConcurrentEffect[F],
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

  private val featureListRoute: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(featureListEndpoint)(_ =>
      toRoute(featureRegistry.featureList())
    )

  private val updateFeatureRoute: HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(updateFeatureEndpoint)(request =>
      toRoute(featureRegistry.update(request.featureName, request.enable).map(_ => StatusCode.Ok))
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
        code,
        FeatureRegistryError(
          code = code.code,
          reason = reason.getLocalizedMessage
        )
      )
    )

  val endpoints = List(
    featureListEndpoint,
    updateFeatureEndpoint
  )

  val route = featureListRoute.combineK(updateFeatureRoute).orNotFound
}

object Http4sFeatureRegistryRoutes {
  def apply[F[_]: ContextShift: Timer](featureRegistry: FeatureRegistry[F])(implicit
    F: ConcurrentEffect[F],
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): Http4sFeatureRegistryRoutes[F] = new Http4sFeatureRegistryRoutes[F](featureRegistry)
}
