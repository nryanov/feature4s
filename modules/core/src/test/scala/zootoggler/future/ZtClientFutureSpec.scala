package zootoggler.future

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import zootoggler.ZkTestServer
import zootoggler.core.configuration.{FeatureConfiguration, RetryPolicyType, ZtConfiguration}

import scala.concurrent.ExecutionContext.Implicits.global

class ZtClientFutureSpec extends ZkTestServer with ScalaFutures {

  "ZtClient" should {
    "register new feature" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val featureCfg = FeatureConfiguration("/features")
      val client = ZtClientFuture(cfg, featureCfg)

      val feature = client.register("test", "name1").flatMap(_.value)

      whenReady(feature, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe "test"
      }
    }

    "get actual feature value when register already existing feature" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val featureCfg = FeatureConfiguration("/features")
      val client = ZtClientFuture(cfg, featureCfg)

      val accessor = for {
        _ <- client.register("actualValue", "name2")
        r <- client.register("defaultValue", "name2")
      } yield r

      val feature = accessor.flatMap(_.value)

      whenReady(feature, PatienceConfiguration.Timeout(Span(5, Seconds))) { f =>
        f shouldBe "actualValue"
      }
    }

    "update feature value" in {
      val cfg = ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
      val featureCfg = FeatureConfiguration("/features")
      val client = ZtClientFuture(cfg, featureCfg)

      val accessorF = client.register("initialValue", "name3")

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
  }
}
