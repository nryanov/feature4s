package feature4s.redis.redisson.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.redisson.RedissonClientCreator
import zio.Task

import scala.concurrent.Future

class RedissonZioAsyncFeatureRegistrySpec
    extends FeatureRegistrySpec[Task]
    with RedissonClientCreator
    with ZioBaseSpec {
  override def featureRegistry(): FeatureRegistry[Task] =
    RedissonZioAsyncFeatureRegistry.useClient(redisClient, DefaultNamespace)

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
