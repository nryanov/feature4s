package feature4s.tapir.zio

import feature4s.{ClientError, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.tapir.{BaseRoutes, Configuration, FeatureRegistryError}
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import zio._
import zio.clock.Clock
import zio.interop.catz._

final class ZioFeatureRegistryRoutes(
  featureRegistry: FeatureRegistry[Task],
  configuration: Configuration
)(implicit
  errorJsonCodec: JsonCodec[FeatureRegistryError],
  featureCodec: JsonCodec[FeatureState],
  featureListCodec: JsonCodec[List[FeatureState]]
) extends BaseRoutes {

  private val featureListRoute =
    featureListEndpoint.serverLogic[Task](_ => toRoute(featureRegistry.featureList()))

  private val enableFeatureRoute = enableFeatureEndpoint.serverLogic[Task](featureName =>
    toRoute(featureRegistry.update(featureName, enable = true).as(StatusCode.Ok))
  )

  private val disableFeatureRoute = disableFeatureEndpoint.serverLogic[Task](featureName =>
    toRoute(featureRegistry.update(featureName, enable = false).as(StatusCode.Ok))
  )

  private val deleteFeatureRoute = deleteFeatureEndpoint.serverLogic[Task](featureName =>
    toRoute(
      featureRegistry.remove(featureName).map(result => if (result) StatusCode.Ok else StatusCode.NotFound)
    )
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

  val route: HttpRoutes[ZIO[Any with Has[Clock.Service], Throwable, *]] = {
    val routes = {
      if (configuration.allowRemove) {
        List(featureListRoute, enableFeatureRoute, disableFeatureRoute, deleteFeatureRoute)
      } else {
        List(featureListRoute, enableFeatureRoute, disableFeatureRoute)
      }
    }

    ZHttp4sServerInterpreter.from(routes).toRoutes
  }
}

object ZioFeatureRegistryRoutes {
  def apply(
    featureRegistry: FeatureRegistry[Task],
    configuration: Configuration = Configuration.Default
  )(implicit
    errorJsonCodec: JsonCodec[FeatureRegistryError],
    featureCodec: JsonCodec[FeatureState],
    featureListCodec: JsonCodec[List[FeatureState]]
  ): ZioFeatureRegistryRoutes =
    new ZioFeatureRegistryRoutes(featureRegistry, configuration)
}
