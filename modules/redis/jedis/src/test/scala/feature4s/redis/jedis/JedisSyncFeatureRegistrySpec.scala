package feature4s.redis.jedis

import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}

import scala.concurrent.Future
import scala.util.Try

class JedisSyncFeatureRegistrySpec extends FeatureRegistrySpec[Id] with JedisClientCreator {
  override def featureRegistry(): FeatureRegistry[Id] = JedisSyncFeatureRegistry(jedisPool, "test")

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))
}
