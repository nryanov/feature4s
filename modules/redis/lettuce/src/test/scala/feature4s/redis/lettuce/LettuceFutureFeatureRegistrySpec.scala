package feature4s.redis.lettuce

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.redis.DefaultNamespace

import scala.concurrent.Future

class LettuceFutureFeatureRegistrySpec extends FeatureRegistrySpec[Future] with LettuceClient {
  override def featureRegistry(): FeatureRegistry[Future] =
    LettuceFutureFeatureRegistry(redisClient, DefaultNamespace)

  override def toFuture[A](v: => Future[A]): Future[A] = v
}
