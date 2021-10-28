package feature4s.redis.lettuce.cats

import cats.effect.IO
import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.cats.CatsBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.lettuce.LettuceClientCreator

import scala.concurrent.Future

class LettuceCatsAsyncFeatureRegistrySpec extends FeatureRegistrySpec[IO] with CatsBaseSpec with LettuceClientCreator {
  override def featureRegistry(): FeatureRegistry[IO] =
    LettuceCatsAsyncFeatureRegistry.useConnection(redisClient.connect(), DefaultNamespace)

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()
}
