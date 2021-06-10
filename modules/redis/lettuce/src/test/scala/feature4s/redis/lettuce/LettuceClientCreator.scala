package feature4s.redis.lettuce

import feature4s.redis.{RedisBackend, RedisContainer}
import io.lettuce.core.RedisClient
import io.lettuce.core.resource.DefaultClientResources
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait LettuceClientCreator extends RedisBackend with BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>
  var redisClient: RedisClient = _

  override def afterContainersStart(redis: RedisContainer): Unit = {
    val clientResources =
      DefaultClientResources.builder().ioThreadPoolSize(2).computationThreadPoolSize(2).build()
    redisClient = RedisClient.create(
      clientResources,
      s"redis://${redis.containerIpAddress}:${redis.mappedPort(6379)}"
    )
  }

  abstract override protected def afterAll(): Unit = {
    redisClient.shutdown()
    super.afterAll()
  }

  abstract override protected def afterEach(): Unit = {
    super.afterEach()
    val connection = redisClient.connect()
    try connection.sync().flushall()
    finally connection.close()
  }
}
