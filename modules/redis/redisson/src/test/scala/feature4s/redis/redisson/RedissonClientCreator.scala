package feature4s.redis.redisson

import feature4s.redis.{RedisBackend, RedisContainer}
import org.redisson.Redisson
import org.redisson.config.Config
import org.redisson.api.{RedissonClient => JRedissonClient}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait RedissonClientCreator extends RedisBackend with BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>
  var redisClient: JRedissonClient = _

  override def afterContainersStart(redis: RedisContainer): Unit = {
    val config = new Config()
    config
      .useSingleServer()
      .setTimeout(1000000)
      .setConnectionMinimumIdleSize(1)
      .setConnectionPoolSize(2)
      .setAddress(s"redis://${redis.containerIpAddress}:${redis.mappedPort(6379)}")

    redisClient = Redisson.create(config)
  }

  override protected def afterAll(): Unit = {
    redisClient.shutdown()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    redisClient.getKeys.flushall()
  }
}
