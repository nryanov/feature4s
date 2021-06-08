package feature4s

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

trait FeatureRegistrySpec[F[_]]
    extends AsyncFunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = ExecutionContext.global

  def featureRegistry(): FeatureRegistry[F]

  def toFuture[A](v: F[A]): Future[A]

  test("register new feature") {
    val registry = featureRegistry()

    for {
      feature <- toFuture(registry.register("test", enable = true, None))
      isEnable <- toFuture(feature.isEnable())
    } yield isEnable shouldBe true
  }

  test("try to register already existing feature -- should not update value") {
    val registry = featureRegistry()

    for {
      _ <- toFuture(registry.register("test", enable = false, None))
      feature <- toFuture(registry.register("test", enable = true, None))
      isEnable <- toFuture(feature.isEnable())
    } yield isEnable shouldBe false
  }

  test("recreate not existing feature -- should register new feature") {
    val registry = featureRegistry()

    for {
      feature <- toFuture(registry.recreate("test", enable = true, None))
      isEnable <- toFuture(feature.isEnable())
    } yield isEnable shouldBe true
  }

  test("recreate feature -- update value and description") {
    val registry = featureRegistry()

    for {
      _ <- toFuture(registry.register("test", enable = false, None))
      feature <- toFuture(registry.recreate("test", enable = true, None))
      isEnable <- toFuture(feature.isEnable())
    } yield isEnable shouldBe true
  }

  test("update value") {
    val registry = featureRegistry()

    for {
      feature <- toFuture(registry.recreate("test", enable = true, None))
      isEnable <- toFuture(feature.isEnable())
      _ <- toFuture(registry.update("test", enable = false))
      isEnableUpdated <- toFuture(feature.isEnable())
    } yield {
      isEnable shouldBe true
      isEnableUpdated shouldBe false
    }
  }

//  test("should fail when update value of not existing feature ") {
//    ???
//  }
//
//  test("update description") {
//    ???
//  }
//
//  test("should fail when update description of not existing feature ") {
//    ???
//  }
//
//  test("get feature list") {
//    ???
//  }
//
//  test("remove feature") {
//    ???
//  }
//
//  test("remove not existing feature") {
//    ???
//  }
//
//  test("is exist should return true if feature exists") {
//    ???
//  }
//
//  test("is exist should return false if feature does not exist") {
//    ???
//  }
}
