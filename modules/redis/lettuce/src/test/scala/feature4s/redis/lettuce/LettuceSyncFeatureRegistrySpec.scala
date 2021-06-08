package feature4s.redis.lettuce

import feature4s.redis.DefaultNamespace
import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}

import scala.concurrent.Future

class LettuceSyncFeatureRegistrySpec extends FeatureRegistrySpec[Id] with LettuceClient {
  override def featureRegistry(): FeatureRegistry[Id] =
    LettuceSyncFeatureRegistry(redisClient, DefaultNamespace)

  override def toFuture[A](v: Id[A]): Future[A] = Future.successful(v)
}
