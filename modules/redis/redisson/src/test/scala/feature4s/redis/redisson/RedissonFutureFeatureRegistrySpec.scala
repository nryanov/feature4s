package feature4s.redis.redisson

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.redis.DefaultNamespace

import scala.concurrent.Future

class RedissonFutureFeatureRegistrySpec
    extends FeatureRegistrySpec[Future]
    with RedissonClientCreator {
  override def featureRegistry(): FeatureRegistry[Future] =
    RedissonFutureFeatureRegistry(redisClient, DefaultNamespace)

  override def toFuture[A](v: => Future[A]): Future[A] = v
}
