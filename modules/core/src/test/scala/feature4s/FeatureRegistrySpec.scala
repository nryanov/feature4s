package feature4s

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

trait FeatureRegistrySpec[F[_]]
    extends AsyncFunSuite
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = ExecutionContext.global

  def featureRegistry(): FeatureRegistry[F]

  def toFuture[A](v: => F[A]): Future[A]

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
      feature <- toFuture(registry.register("test", enable = true, None))
      isEnable <- toFuture(feature.isEnable())
      _ <- toFuture(registry.update("test", enable = false))
      isEnableUpdated <- toFuture(feature.isEnable())
    } yield {
      isEnable shouldBe true
      isEnableUpdated shouldBe false
    }
  }

  test("should fail when update value of not existing feature ") {
    val registry = featureRegistry()

    whenReady(toFuture(registry.update("test", enable = false)).failed)(r => r shouldBe FeatureNotFound("test"))
  }

  test("get feature list") {
    val registry = featureRegistry()

    for {
      _ <- toFuture(registry.register("test_1", enable = true, None))
      _ <- toFuture(registry.register("test_2", enable = false, None))
      features <- toFuture(registry.featureList())
    } yield features.sortBy(_.name) shouldBe List(
      FeatureState("test_1", isEnable = true, None),
      FeatureState("test_2", isEnable = false, None)
    )
  }

  test("remove feature") {
    val registry = featureRegistry()

    for {
      feature <- toFuture(registry.register("test", enable = true, None))
      isEnabled <- toFuture(feature.isEnable())
      delete <- toFuture(registry.remove("test"))
    } yield {
      isEnabled shouldBe true
      delete shouldBe true
    }
  }

  test("should fail when try to get value of deleted feature") {
    val registry = featureRegistry()

    val f = for {
      feature <- toFuture(registry.register("test", enable = true, None))
      _ <- toFuture(registry.remove("test"))
      _ <- toFuture(feature.isEnable())
    } yield ()

    whenReady(f.failed)(r => r shouldBe FeatureNotFound("test"))
  }

  test("remove not existing feature") {
    val registry = featureRegistry()

    for {
      delete <- toFuture(registry.remove("test"))
    } yield delete shouldBe false
  }

  test("is exist should return true if feature exists") {
    val registry = featureRegistry()

    for {
      _ <- toFuture(registry.register("test", enable = true, None))
      exists <- toFuture(registry.isExist("test"))
    } yield exists shouldBe true
  }

  test("is exist should return false if feature does not exist") {
    val registry = featureRegistry()

    for {
      exists <- toFuture(registry.isExist("test"))
    } yield exists shouldBe false
  }
}
