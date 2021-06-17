package feature4s.redis.lettuce.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.lettuce.LettuceClientCreator
import zio._

import scala.concurrent.Future

class LettuceZioAsyncFeatureRegistrySpec
    extends FeatureRegistrySpec[Task]
    with LettuceClientCreator
    with ZioBaseSpec {
  override def featureRegistry(): FeatureRegistry[Task] =
    LettuceZioAsyncFeatureRegistry.useClient(redisClient.connect(), DefaultNamespace)

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
