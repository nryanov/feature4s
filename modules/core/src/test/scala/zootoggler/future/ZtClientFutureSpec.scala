package zootoggler.future

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
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
        f shouldBe "test"
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
        f shouldBe "actualValue"
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
          initial shouldBe "initialValue"
          updated shouldBe "updatedValue"
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
          initial shouldBe "initialValue"
          updated shouldBe "updatedValue"
          updatedCache shouldBe "updatedValue"
      }
    }
  }
}
