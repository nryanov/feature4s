package feature4s.tapir.akka

import feature4s._
import feature4s.tapir.FeatureRegistryError
import akka.http.javadsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import org.scalatest.EitherValues

import scala.concurrent._

class AkkaFeatureRegistryRoutesSpec
    extends AnyFunSuite
    with Matchers
    with EitherValues
    with MockFactory
    with ScalatestRouteTest {

  test("success response for feature list") {
    val featureRegistry = mock[FeatureRegistry[Future]]

    (featureRegistry.featureList _)
      .expects()
      .returning(Future.successful(List(FeatureState("test", isEnable = true, None))))

    val routes = AkkaFeatureRegistryRoutes(featureRegistry)

    Get("/features") ~> routes.route ~> check {
      decode[List[FeatureState]](responseAs[String]).value shouldBe List(
        FeatureState("test", isEnable = true, None)
      )
      status shouldBe StatusCodes.OK
    }
  }

  test("success response for feature value update") {
    val featureRegistry = mock[FeatureRegistry[Future]]

    (featureRegistry.update _).expects("test", false).returning(Future.successful(()))

    val routes = AkkaFeatureRegistryRoutes(featureRegistry)

    Put("/features/test/disable") ~> routes.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("feature not found") {
    val featureRegistry = mock[FeatureRegistry[Future]]

    (featureRegistry.update _).expects("test", false).returning(Future.failed(FeatureNotFound("test")))

    val routes = AkkaFeatureRegistryRoutes(featureRegistry)

    Put("/features/test/disable") ~> routes.route ~> check {
      status shouldBe StatusCodes.NOT_FOUND
      decode[FeatureRegistryError](responseAs[String]).value shouldBe FeatureRegistryError(
        StatusCodes.NOT_FOUND.intValue(),
        FeatureNotFound("test").getLocalizedMessage
      )
    }
  }
}
