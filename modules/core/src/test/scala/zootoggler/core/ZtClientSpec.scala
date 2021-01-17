package zootoggler.core

import org.scalatest.OptionValues
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import zootoggler.ZkTestServer
import zootoggler.core.configuration.RetryPolicyType.Exponential
import zootoggler.core.configuration.ZtConfiguration

class ZtClientSpec extends ZkTestServer with OptionValues with Eventually {
  "ZtClient" should {
    "register feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      val feature: Option[String] = client.register("test", "name1").flatMap(_.value).toOption

      feature.value shouldBe "test"
    }

    "register feature in custom namespace" in {
      val cfg = ZtConfiguration(
        server.getConnectString,
        "/features",
        Some("mynamespace"),
        Exponential(1000, 5)
      )
      val client = ZtClientBasic(cfg)

      val feature: Option[String] =
        client.register("test", "customNamespace").flatMap(_.value).toOption

      feature.value shouldBe "test"
    }

    "successfully return feature accessor for already existed feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      // register new feature
      val featureOne = client.register("test", "name2").flatMap(_.value).toOption
      // try to register the same feature again
      val featureTwo = client.register("test", "name2").flatMap(_.value).toOption

      // both attempts should be success
      featureOne.value shouldBe "test"
      featureTwo.value shouldBe "test"
    }

    "update feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      val feature: Attempt[FeatureAccessor[Attempt, String]] = client.register("test", "name3")

      feature.require.value.toOption.value shouldBe "test"
      feature.require.update("updatedValue").toOption.value shouldBe true

      eventuallyWithTimeout(
        feature.require.value.toOption.value shouldBe "updatedValue"
      )
    }

    "get feature value" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      val feature: Attempt[FeatureAccessor[Attempt, String]] = client.register("test", "name4")

      feature.require.value.toOption.value shouldBe "test"
    }

    "remove feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      val feature: Attempt[FeatureAccessor[Attempt, String]] = client.register("test", "name5")

      feature.require.value.toOption.value shouldBe "test"
      client.remove("name5").toOption.value shouldBe true
    }

    "recreate feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      // register new feature
      val featureOne = client.register("initialValue", "name6").flatMap(_.value).toOption
      // fully recreate feature if exists (get data with version -> delete by specific version -> create new)
      val featureTwo = client.recreate("recreatedValue", "name6").flatMap(_.value).toOption

      // both attempts should be success
      featureOne.value shouldBe "initialValue"
      featureTwo.value shouldBe "recreatedValue"
    }

    "return error when trying to get deleted feature" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      val feature: Attempt[FeatureAccessor[Attempt, String]] = client.register("test", "name7")

      feature.require.value.toOption.value shouldBe "test"
      client.remove("name7")
      // fail to get deleted feature value
      eventuallyWithTimeout(
        feature.require.value.isFailure shouldBe true
      )
    }

    "check if feature exists" in {
      val cfg = ZtConfiguration(server.getConnectString, "/features", Exponential(1000, 5))
      val client = ZtClientBasic(cfg)

      client.register("test", "name8")

      eventuallyWithTimeout(
        client.isExist("name8").toOption.value shouldBe true
      )

      client.remove("name8")

      eventuallyWithTimeout(
        client.isExist("name8").toOption.value shouldBe false
      )
    }
  }

  private def eventuallyWithTimeout[T](f: => T): T =
    eventually(timeout(Span(5, Seconds)), interval(Span(100, Millis)))(f)
}
