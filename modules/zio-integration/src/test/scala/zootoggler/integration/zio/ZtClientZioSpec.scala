package zootoggler.integration.zio

import java.time.Duration

import zio.{Has, ZLayer}
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, timeout}
import zio.test.{DefaultRunnableSpec, ZSpec, assertM, suite, testM}
import zootoggler.ZookeeperTestServer
import zootoggler.ZookeeperTestServer.Zookeeper
import zootoggler.core.Feature
import zootoggler.core.configuration.{RetryPolicyType, ZtConfiguration}
import zootoggler.integration.zio.ZtClientZio.ZtClientEnv

object ZtClientZioSpec extends DefaultRunnableSpec {
  val zookeeper = Blocking.live >>> ZookeeperTestServer.zookeeper()
  val cfg: ZLayer[Zookeeper, Throwable, Has[ZtConfiguration]] =
    ZLayer.fromService(server =>
      ZtConfiguration(server.getConnectString, RetryPolicyType.Exponential(1000, 5))
    )
  val client = (cfg ++ Blocking.live) >>> ZtClientZio.live
  val testEnv: ZLayer[Any, Throwable, ZtClientEnv] = zookeeper >>> client

  val timeoutAspect = timeout(Duration.ofSeconds(5))

  override def spec: ZSpec[zio.test.environment.TestEnvironment, Any] =
    (suite("ZtClientZio")(
      testM("register new feature") {
        val effect = ZtClientZio.register("test", "/test1").flatMap(_.value)
        assertM(effect)(equalTo("test"))
      },
      testM("get actual feature value when register already existing feature") {
        val effect = for {
          _ <- ZtClientZio.register("actualValue", "/test2")
          feature <- ZtClientZio.register("defaultValue", "/test2").flatMap(_.value)
        } yield feature

        assertM(effect)(equalTo("actualValue"))
      },
      testM("update feature value") {
        val effect = for {
          accessor <- ZtClientZio.register("initialValue", "/test3")
          initialValue <- accessor.value
          _ <- accessor.update("updatedValue")
          updatedValue <- accessor.value
        } yield (initialValue, updatedValue)

        assertM(effect)(
          equalTo(
            (
              "initialValue",
              "updatedValue"
            )
          )
        )
      },
      testM("update feature value and cache") {
        val effect = for {
          accessor <- ZtClientZio.register("initialValue", "/test4")
          initialCache = accessor.cachedValue
          _ <- accessor.update("updatedValue")
          updatedValue <- accessor.value
          updatedCache = accessor.cachedValue
        } yield (initialCache, updatedValue, updatedCache)

        assertM(effect)(
          equalTo(
            (
              "initialValue",
              "updatedValue",
              "updatedValue"
            )
          )
        )
      }
    ) @@ sequential).provideCustomLayerShared(testEnv.orDie)
}
