package zootoggler.integration.cats

import zootoggler.core.configuration.{RetryPolicyType, ZtConfiguration}
import zootoggler.{IOSpec, ZkTestServer}

import scala.concurrent.duration._

class ZtClientCatsSpec extends IOSpec with ZkTestServer {
  "ZtClientCats" should {
    "register new feature" in runF {
      val cfg =
        ZtConfiguration(server.getConnectString, "/features", RetryPolicyType.Exponential(1000, 5))

      ZtClientCats
        .resource[F](cfg)
        .use { client =>
          for {
            feature <- client.register("test", "name1").flatMap(_.value)
          } yield assertResult("test")(feature)
        }
        .timeout(5 seconds)
    }

    "get actual feature value when register already existing feature" in runF {
      val cfg =
        ZtConfiguration(server.getConnectString, "/features", RetryPolicyType.Exponential(1000, 5))

      ZtClientCats
        .resource[F](cfg)
        .use { client =>
          for {
            _ <- client.register("actualValue", "name2")
            feature <- client.register("defaultValue", "name2")
            value <- feature.value
          } yield assertResult("actualValue")(value)
        }
        .timeout(5 seconds)
    }

    "update feature value" in runF {
      val cfg =
        ZtConfiguration(server.getConnectString, "/features", RetryPolicyType.Exponential(1000, 5))

      ZtClientCats
        .resource[F](cfg)
        .use { client =>
          for {
            accessorF <- client.register("initialValue", "name3")
            initialValue <- accessorF.value
            _ <- accessorF.update("updatedValue")
            updatedValue <- accessorF.value
          } yield {
            assertResult("initialValue")(initialValue)
            assertResult("updatedValue")(updatedValue)
          }
        }
        .timeout(5 seconds)
    }
  }
}
