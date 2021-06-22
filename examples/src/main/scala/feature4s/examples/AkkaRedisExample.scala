package feature4s.examples

// akka
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
// feature4s
import feature4s.redis.lettuce.LettuceFutureFeatureRegistry
import feature4s.tapir.akka.AkkaFeatureRegistryRoutes
import feature4s.tapir.json.circe.codecs._
// lettuce
import io.lettuce.core.RedisClient
// tapir
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.concurrent.Await
import scala.util.{Failure, Success}

import scala.io.StdIn
import scala.concurrent.duration._

/**
 * localhost:8080/test
 * localhost:8080/docs
 */
object AkkaRedisExample {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("example")
    import actorSystem.dispatcher

    val redisClient = RedisClient.create("redis://localhost:6379")
    val redisConnection = redisClient.connect()
    val featureRegistry = LettuceFutureFeatureRegistry.useConnection(redisConnection, "features")
    val featureRegistryRoutes = AkkaFeatureRegistryRoutes(featureRegistry)

    val feature = Await.result(
      featureRegistry.register("feature_1", enable = false, Some("Example feature")),
      5.seconds
    )

    val route =
      path("test") {
        get {
          onComplete(feature.isEnable()) {
            case Failure(exception) =>
              complete(InternalServerError, s"Error: ${exception.getLocalizedMessage}")
            case Success(isEnable) =>
              if (isEnable) {
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "feature ON"))
              } else {
                complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "feature OFF"))
              }
          }
        }
      }

    val openApiDocs: OpenAPI =
      OpenAPIDocsInterpreter.toOpenAPI(
        featureRegistryRoutes.endpoints,
        "Example",
        "0.0.1"
      )
    val openApiYml: String = openApiDocs.toYaml

    val routes = {
      import akka.http.scaladsl.server.Directives._
      concat(route, featureRegistryRoutes.route, new SwaggerAkka(openApiYml).routes)
    }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)

    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete { _ =>
      redisConnection.close()
      actorSystem.terminate()
    }
  }
}
