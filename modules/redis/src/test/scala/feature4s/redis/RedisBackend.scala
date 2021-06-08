package feature4s.redis

import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.Suite

trait RedisBackend extends TestContainerForAll { self: Suite =>
  override val containerDef: RedisContainer.Def = RedisContainer.Def()
}
