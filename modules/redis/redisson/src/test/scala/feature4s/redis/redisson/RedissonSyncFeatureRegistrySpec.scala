package feature4s.redis.redisson

import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}
import feature4s.redis.DefaultNamespace

import scala.concurrent.Future
import scala.util.Try

class RedissonSyncFeatureRegistrySpec extends FeatureRegistrySpec[Id] with RedissonClientCreator {
  override def featureRegistry(): FeatureRegistry[Id] =
    RedissonSyncFeatureRegistry(redisClient, DefaultNamespace)

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))
}
