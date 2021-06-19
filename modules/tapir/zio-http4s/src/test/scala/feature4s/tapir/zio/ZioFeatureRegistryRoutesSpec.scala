package feature4s.tapir.zio

import feature4s.{FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.tapir.FeatureRegistryError
import io.circe.Decoder
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Assertion, EitherValues}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import zio._
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s._
import zio.clock.Clock
import zio.interop.catz._

class ZioFeatureRegistryRoutesSpec
    extends AnyFunSuite
    with Matchers
    with EitherValues
    with MockFactory {

  val runtime = zio.Runtime.default
  type ResultT[A] = ZIO[Any with Clock, Throwable, A]

  def check[A](
    actualResp: ResultT[Option[Response[ResultT[*]]]],
    expectedStatus: Status,
    expectedBody: A
  )(implicit
    decoder: Decoder[A]
  ): Assertion = {
    val response = runtime.unsafeRun(actualResp).get
    val status = response.status
    val body = response.body.compile.toVector.map(x => x.map(_.toChar).mkString(""))
    val data = decode[A](runtime.unsafeRun(body)).value

    assertResult(expectedBody)(data)
    assertResult(expectedStatus)(status)
  }

  def check(
    actualResp: ResultT[Option[Response[ResultT[*]]]],
    expectedStatus: Status
  ): Assertion = {
    val response = runtime.unsafeRun(actualResp).get

    assertResult(expectedStatus)(response.status)
  }

  test("success response for feature list") {
    val featureRegistry = mock[FeatureRegistry[Task]]

    (featureRegistry.featureList _)
      .expects()
      .returning(Task.succeed(List(FeatureState("test", isEnable = true, None))))

    val routes = ZioFeatureRegistryRoutes(featureRegistry)
    val request =
      Request[ResultT](method = Method.GET, uri = Uri(path = s"/features"))

    val result = routes.route.run(request).value

    check[List[FeatureState]](result, Status.Ok, List(FeatureState("test", isEnable = true, None)))
  }

  test("success response for feature value update") {
    val featureRegistry = mock[FeatureRegistry[Task]]

    (featureRegistry.update _).expects("test", false).returning(Task.unit)

    val routes = ZioFeatureRegistryRoutes(featureRegistry)
    val request = Request[ResultT](method = Method.PUT, uri = Uri(path = s"/features/test/disable"))

    val result = routes.route.run(request).value

    check(result, Status.Ok)
  }

  test("feature not found") {
    val featureRegistry = mock[FeatureRegistry[Task]]

    (featureRegistry.update _).expects("test", false).returning(Task.fail(FeatureNotFound("test")))

    val routes = ZioFeatureRegistryRoutes(featureRegistry)
    val request = Request[ResultT](method = Method.PUT, uri = Uri(path = s"/features/test/disable"))

    val result = routes.route.run(request).value

    check[FeatureRegistryError](
      result,
      Status.NotFound,
      FeatureRegistryError(Status.NotFound.code, FeatureNotFound("test").getLocalizedMessage)
    )
  }
}
