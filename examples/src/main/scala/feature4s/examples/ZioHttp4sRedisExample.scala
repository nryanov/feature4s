package feature4s.examples

// feature4s
import feature4s.{Feature, FeatureRegistry}
import feature4s.tapir.zio.ZioFeatureRegistryRoutes
import feature4s.redis.lettuce.zio.LettuceZioFeatureRegistry
import sttp.tapir.swagger.http4s.SwaggerHttp4s
// lettuce
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
// http4s
import org.http4s._
import org.http4s.server.Router
import org.http4s.syntax.kleisli._
import org.http4s.server.blaze.BlazeServerBuilder
// cats
import cats.syntax.semigroupk._
// zio
import zio._
import zio.clock.Clock
import zio.interop.catz._
import zio.blocking.Blocking
import sttp.tapir.ztapir._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
// swagger
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml._
// codecs
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object ZioHttp4sRedisExample extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    createRedisClient.use { connection =>
      for {
        featureRegistry <- createFeatureRegistry(connection)
        feature <- featureRegistry.register("feature_1", enable = false, Some("Example feature"))
        testRoute = createTestRoute(feature)
        featureRegistryRoutes <- createFeatureRegistryRoutes(featureRegistry)
        _ <- runServer(testRoute, featureRegistryRoutes)
      } yield ExitCode.success
    }.orDie

  def createRedisClient: ZManaged[Any, Throwable, StatefulRedisConnection[String, String]] =
    Managed.make(
      Task.effect(RedisClient.create("redis://localhost:6379").connect())
    )(connection => Task.effect(connection.close()).orDie)

  def createFeatureRegistry(
    connection: StatefulRedisConnection[String, String]
  ): ZIO[Blocking, Nothing, FeatureRegistry[Task]] =
    for {
      blocking <- ZIO.service[Blocking.Service]
    } yield LettuceZioFeatureRegistry.useConnection(connection, "features", blocking)

  def createTestRoute(feature: Feature[Task]) =
    ZHttp4sServerInterpreter
      .from(endpoint.get.in("test").out(stringBody)) { _ =>
        Task
          .ifM(feature.isEnable())(
            Task.effectTotal(s"Feature ${feature.name} is enabled"),
            Task.effectTotal(s"Feature ${feature.name} is disabled")
          )
          .orDie
      }
      .toRoutes

  def createFeatureRegistryRoutes(
    featureRegistry: FeatureRegistry[Task]
  ): UIO[ZioFeatureRegistryRoutes] =
    Task.effectTotal(ZioFeatureRegistryRoutes(featureRegistry))

  def runServer(
    routes: HttpRoutes[RIO[Clock, *]],
    featureRegistryRoutes: ZioFeatureRegistryRoutes
  ): ZIO[zio.ZEnv, Throwable, Unit] = {
    val yaml =
      OpenAPIDocsInterpreter.toOpenAPI(featureRegistryRoutes.endpoints, "Example", "1.0").toYaml

    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[RIO[Clock, *]](runtime.platform.executor.asEC)
        .bindHttp(8080, "localhost")
        .withHttpApp(
          Router(
            "/" -> routes
              .combineK(featureRegistryRoutes.route)
              .combineK(new SwaggerHttp4s(yaml).routes)
          ).orNotFound
        )
        .serve
        .compile
        .drain
    }
  }
}
