package feature4s.redis.jedis

import feature4s.redis.{RedisBackend, RedisContainer}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import redis.clients.jedis.{Jedis, JedisPool}

trait JedisClient extends RedisBackend with BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>
  var jedisPool: JedisPool = _

  override def afterContainersStart(redis: RedisContainer): Unit = {
    val poolConfig = new GenericObjectPoolConfig[Jedis]()
    poolConfig.setMinIdle(1)
    poolConfig.setMaxIdle(2)
    poolConfig.setMaxTotal(2)
    jedisPool = new JedisPool(poolConfig, redis.containerIpAddress, redis.mappedPort(6379))
  }

  override protected def afterAll(): Unit = {
    jedisPool.close()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    val jedis = jedisPool.getResource
    try jedis.flushAll()
    finally jedis.close()
  }
}
