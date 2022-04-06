package feature4s.examples

// cats
import cats.syntax.flatMap._
import cats.effect._
import cats.syntax.semigroupk._
import cats.syntax.either._
// feature4s
import feature4s.{Feature, FeatureRegistry}
import feature4s.tapir.json.circe.codecs._
import feature4s.redis.lettuce.cats.LettuceCatsFeatureRegistry
import feature4s.tapir.http4s.Http4sFeatureRegistryRoutes
// lettuce
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
// http4s
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
// tapir
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.concurrent.ExecutionContext
import cats.effect.{ Resource, Temporal }

object Http4sRedisExample extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    redisClientResource[IO].flatMap(createFeatureRegistry[IO]).use { featureRegistry =>
      for {
        feature <- featureRegistry.register("feature_1", enable = false, Some("Example feature"))
        testRoute = createTestRoute(feature)
        featureRegistryRoutes = createFeatureRouteWrapper(featureRegistry)

        openApiDocs = OpenAPIDocsInterpreter.toOpenAPI(featureRegistryRoutes.endpoints, "Example", "1.0.0")
        routes = testRoute
          .combineK(featureRegistryRoutes.route)
          .combineK(new SwaggerHttp4s(openApiDocs.toYaml).routes[IO])

        _ <- BlazeServerBuilder
          .apply[IO](ExecutionContext.global)
          .bindHttp(8080, "localhost")
          .withHttpApp(Router("/" -> routes).orNotFound)
          .serve
          .compile
          .drain
      } yield ExitCode.Success
    }

  def redisClientResource[F[_]](implicit
    F: Sync[F]
  ): Resource[F, StatefulRedisConnection[String, String]] =
    Resource.make(
      F.delay(RedisClient.create("redis://localhost:6379")).flatMap(client => F.delay(client.connect()))
    )(connection => F.delay(connection.close()))

  def createFeatureRegistry[F[_]: ContextShift](
    connection: StatefulRedisConnection[String, String]
  )(implicit
    F: Sync[F]
  ): Resource[F, FeatureRegistry[F]] =
    Resource.unit[F].map(blocker => LettuceCatsFeatureRegistry.useConnection(connection, "features", blocker))

  def createTestRoute[F[_]: ContextShift: Temporal](
    feature: Feature[F]
  )(implicit F: Concurrent[F]): HttpRoutes[F] =
    Http4sServerInterpreter.toRoutes(endpoint.get.in("test").out(stringBody))(_ =>
      F.ifM(feature.isEnable())(
        ifTrue = F.pure(s"Feature ${feature.name} is enabled".asRight[Unit]),
        ifFalse = F.pure(s"Feature ${feature.name} is disabled".asRight[Unit])
      )
    )

  def createFeatureRouteWrapper[F[_]: ContextShift: Temporal](
    featureRegistry: FeatureRegistry[F]
  )(implicit F: ConcurrentEffect[F]): Http4sFeatureRegistryRoutes[F] =
    Http4sFeatureRegistryRoutes(featureRegistry)
}
