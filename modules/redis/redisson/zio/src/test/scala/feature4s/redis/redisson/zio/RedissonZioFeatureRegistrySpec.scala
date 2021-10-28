package feature4s.redis.redisson.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.redisson.RedissonClientCreator
import zio.{Task, ZIO}
import zio.blocking.Blocking

import scala.concurrent.Future

class RedissonZioFeatureRegistrySpec extends FeatureRegistrySpec[Task] with ZioBaseSpec with RedissonClientCreator {
  override def featureRegistry(): FeatureRegistry[Task] =
    runtime.unsafeRun(
      ZIO
        .service[Blocking.Service]
        .map(blocking => RedissonZioFeatureRegistry.useClient(redisClient, DefaultNamespace, blocking))
    )

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
