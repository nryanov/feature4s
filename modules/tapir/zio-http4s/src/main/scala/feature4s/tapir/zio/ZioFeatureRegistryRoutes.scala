package feature4s.tapir.zio

import feature4s.{ClientError, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.tapir.{FeatureRegistryError, UpdateFeatureRequest}
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.ztapir._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio._
import zio.clock.Clock
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
    featureListEndpoint.serverLogic[Task](_ => toRoute(featureRegistry.featureList()))

  private val updateFeatureRoute = updateFeatureEndpoint.serverLogic[Task](request =>
    toRoute(featureRegistry.update(request.featureName, request.enable).as(StatusCode.Ok))
  )

  private def toRoute[A](fa: Task[A]): Task[Either[(StatusCode, FeatureRegistryError), A]] =
    fa.map(Right(_)).catchSome {
      case err: FeatureNotFound => errorResponse[A](StatusCode.NotFound, err)
      case err: ClientError     => errorResponse[A](StatusCode.InternalServerError, err)
      case err: Throwable       => errorResponse[A](StatusCode.BadRequest, err)
    }

  private def errorResponse[A](
    statusCode: StatusCode,
    err: Throwable
  ): Task[Either[(StatusCode, FeatureRegistryError), A]] = Task.left {
    (
      statusCode,
      FeatureRegistryError(code = statusCode.code, reason = err.getLocalizedMessage)
    )
  }

  val endpoints = List(
    featureListEndpoint,
    updateFeatureEndpoint
  )

  val route: HttpRoutes[ZIO[Any with Has[Clock.Service], Throwable, *]] =
    ZHttp4sServerInterpreter.from(List(featureListRoute, updateFeatureRoute)).toRoutes
}

object ZioFeatureRegistryRoutes {
  def apply(featureRegistry: FeatureRegistry[Task])(implicit
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): ZioFeatureRegistryRoutes =
    new ZioFeatureRegistryRoutes(featureRegistry)
}
