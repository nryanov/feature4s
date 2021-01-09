package zootoggler.future

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Second, Seconds, Span}
import zootoggler.ZkTestServer
import zootoggler.core.Feature
import zootoggler.core.configuration.{RetryPolicyType, ZtConfiguration}

import scala.concurrent.ExecutionContext.Implicits.global

class ZtClientFutureSpec extends ZkTestServer with ScalaFutures {

  "ZtClient" should {
    "register new feature" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val feature = client.register("test", "/test1").flatMap(_.value)

      whenReady(feature, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe Feature("test", "/test1")
      }
    }

    "get actual feature value when register already existing feature" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val accessor = for {
        _ <- client.register("actualValue", "/test2")
        r <- client.register("defaultValue", "/test2")
      } yield r

      val feature = accessor.flatMap(_.value)

      whenReady(feature, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe Feature("actualValue", "/test2")
      }
    }

    "update feature value" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val accessorF = client.register("initialValue", "/test3")

      val result = for {
        accessor <- accessorF
        initialValue <- accessor.value
        _ <- accessor.update("updatedValue")
        updatedValue <- accessor.value
      } yield (initialValue, updatedValue)

      whenReady(result, PatienceConfiguration.Timeout(Span(5, Seconds))) {
        case (initial, updated) =>
          initial shouldBe Feature("initialValue", "/test3")
          updated shouldBe Feature("updatedValue", "/test3")
      }
    }

    "update feature value and cache" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val client = ZtClientFuture(cfg)

      val accessorF = client.register("initialValue", "/test4")

      val result = for {
        accessor <- accessorF
        initialCache = accessor.cachedValue
        _ <- accessor.update("updatedValue")
        updatedValue <- accessor.value
        updatedCache = accessor.cachedValue
      } yield (initialCache, updatedValue, updatedCache)

      whenReady(result, PatienceConfiguration.Timeout(Span(5, Seconds))) {
        case (initial, updated, updatedCache) =>
          initial shouldBe Feature("initialValue", "/test4")
          updated shouldBe Feature("updatedValue", "/test4")
          updatedCache shouldBe Feature("updatedValue", "/test4")
      }
    }
  }
}
