package feature4s.tapir.http4s

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Timer}
import feature4s.{FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.tapir.FeatureRegistryError
import io.circe.Decoder
import org.http4s.{Method, Request, Response, Status, Uri}
import org.http4s.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Assertion, EitherValues}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class Http4sFeatureRegistryRoutesSpec
    extends AnyFunSuite
    with Matchers
    with EitherValues
    with MockFactory {

  val catsExecutionContext: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  implicit val cs: ContextShift[IO] = IO.contextShift(catsExecutionContext)
  implicit val timer: Timer[IO] = IO.timer(catsExecutionContext)

  def check[A](actualResp: IO[Response[IO]], expectedStatus: Status, expectedBody: A)(implicit
    decoder: Decoder[A]
  ): Assertion = {
    val response = actualResp.unsafeRunSync()
    val status = response.status
    val data = decode[A](response.as[String].unsafeRunSync()).value

    assertResult(expectedBody)(data)
    assertResult(expectedStatus)(status)
  }

  def check(actualResp: IO[Response[IO]], expectedStatus: Status): Assertion = {
    val response = actualResp.unsafeRunSync()

    assertResult(expectedStatus)(response.status)
  }

  test("success response for feature list") {
    val featureRegistry = mock[FeatureRegistry[IO]]

    (featureRegistry.featureList _)
      .expects()
      .returning(IO.pure(List(FeatureState("test", isEnable = true, None))))

    val routes = Http4sFeatureRegistryRoutes[IO](featureRegistry)
    val request = Request[IO](method = Method.GET, uri = Uri(path = s"/features"))

    val result: IO[Response[IO]] = routes.route.orNotFound.run(request)

    check[List[FeatureState]](result, Status.Ok, List(FeatureState("test", isEnable = true, None)))
  }

  test("success response for feature value update") {
    val featureRegistry = mock[FeatureRegistry[IO]]

    (featureRegistry.update _).expects("test", false).returning(IO.unit)

    val routes = Http4sFeatureRegistryRoutes[IO](featureRegistry)
    val request = Request[IO](method = Method.PUT, uri = Uri(path = s"/features/test/disable"))

    val result: IO[Response[IO]] = routes.route.orNotFound.run(request)

    check(result, Status.Ok)
  }

  test("feature not found") {
    val featureRegistry = mock[FeatureRegistry[IO]]

    (featureRegistry.update _)
      .expects("test", false)
      .returning(IO.raiseError(FeatureNotFound("test")))

    val routes = Http4sFeatureRegistryRoutes[IO](featureRegistry)
    val request = Request[IO](method = Method.PUT, uri = Uri(path = s"/features/test/disable"))

    val result: IO[Response[IO]] = routes.route.orNotFound.run(request)

    check[FeatureRegistryError](
      result,
      Status.NotFound,
      FeatureRegistryError(Status.NotFound.code, FeatureNotFound("test").getLocalizedMessage)
    )
  }
}
