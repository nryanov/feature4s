package feature4s.redis.jedis.cats

import cats.effect.IO
import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.cats.CatsBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.jedis.JedisClientCreator

import scala.concurrent.Future

class JedisCatsFeatureRegistrySpec
    extends FeatureRegistrySpec[IO]
    with CatsBaseSpec
    with JedisClientCreator {
  override def featureRegistry(): FeatureRegistry[IO] =
    JedisCatsFeatureRegistry.useClient(jedisPool, DefaultNamespace, blocker)

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()
}
