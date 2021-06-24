package feature4s.redis

import java.io.File
import java.time.Duration

import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.Suite
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService, WaitingForService}
import org.testcontainers.containers.wait.strategy.{WaitStrategy, WaitStrategyTarget}

trait RedisSentinelBackend extends TestContainerForAll { self: Suite =>

  override val containerDef: DockerComposeContainer.Def = DockerComposeContainer.Def(
    composeFiles = new File(
      Thread.currentThread().getContextClassLoader.getResource("redis-sentinel.yaml").toURI
    ),
    exposedServices = Seq(
      ExposedService("redis", 6379),
      ExposedService("sentinel_1", 26379),
      ExposedService("sentinel_2", 26380),
      ExposedService("sentinel_3", 26381)
    ),
    waitingFor = Some(
      WaitingForService(
        "redis",
        new WaitStrategy {
          override def waitUntilReady(waitStrategyTarget: WaitStrategyTarget): Unit =
            Thread.sleep(10000)

          override def withStartupTimeout(startupTimeout: Duration): WaitStrategy = {
            Thread.sleep(10000)
            this
          }
        }
      )
    )
  )

}
