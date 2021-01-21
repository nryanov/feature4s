package zootoggler.future

import org.scalatest.concurrent.{Eventually, PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import zootoggler.ZkTestServer
import zootoggler.core.configuration.RetryPolicyType.Exponential
import zootoggler.core.configuration.ZtConfiguration

import scala.concurrent.ExecutionContext.Implicits.global

class ZtClientFutureSpec extends ZkTestServer with ScalaFutures with Eventually {
  "ZtClient" should {
    "register feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val feature = client.register("test", "name1").flatMap(_.value)

      whenReady(feature, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe "test"
      }
    }

    "register feature in custom namespace" in {
      val cfg = ZtConfiguration(
        server.getConnectString,
        "/features",
        Some("mynamespace"),
        Exponential(1000, 5)
      )
      val client = ZtClientFuture(cfg)

      val feature =
        client.register("test", "customNamespace").flatMap(_.value)

      whenReady(feature, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe "test"
      }
    }

    "successfully return feature accessor for already existed feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      // register new feature
      val featureOne = client.register("test", "name2").flatMap(_.value)
      // try to register the same feature again
      val featureTwo = client.register("test", "name2").flatMap(_.value)

      val result = for {
        one <- featureOne
        two <- featureTwo
      } yield (one, two)

      // both attempts should be success
      whenReady(result, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe ("test", "test")
      }
    }

    "update feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val f = for {
        accessor <- client.register("test", "name3")
        value <- accessor.value
        _ <- client.update("name3", "updatedValue")
        newValue <- accessor.value
      } yield (value, newValue)

      eventuallyWithTimeout(
        whenReady(f, PatienceConfiguration.Timeout(Span(5, Seconds))) { case (prev, next) =>
          prev shouldBe "test"
          next shouldBe "updatedValue"
        }
      )
    }

    "recreate feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val f = for {
        accessor <- client.register("initialValue", "name4")
        value <- accessor.value
        accessor <- client.recreate("recreatedValue", "name4")
        newValue <- accessor.value
      } yield (value, newValue)

      eventuallyWithTimeout(
        whenReady(f, PatienceConfiguration.Timeout(Span(5, Seconds))) { case (prev, next) =>
          prev shouldBe "initialValue"
          next shouldBe "recreatedValue"
        }
      )
    }
  }

  private def eventuallyWithTimeout[T](f: => T): T =
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis)))(f)
}
