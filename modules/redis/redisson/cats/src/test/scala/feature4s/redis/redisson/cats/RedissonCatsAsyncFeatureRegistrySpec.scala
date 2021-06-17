package feature4s.redis.redisson.cats

import cats.effect.IO
import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.cats.CatsBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.redisson.RedissonClientCreator

import scala.concurrent.Future

class RedissonCatsAsyncFeatureRegistrySpec
    extends FeatureRegistrySpec[IO]
    with RedissonClientCreator
    with CatsBaseSpec {
  override def featureRegistry(): FeatureRegistry[IO] =
    RedissonCatsAsyncFeatureRegistry.useClient(redisClient, DefaultNamespace)

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()
}
