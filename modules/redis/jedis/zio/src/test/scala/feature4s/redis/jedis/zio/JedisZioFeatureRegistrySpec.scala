package feature4s.redis.jedis.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.jedis.JedisClientCreator
import zio.{Task, ZIO}
import zio.blocking.Blocking

import scala.concurrent.Future

class JedisZioFeatureRegistrySpec
    extends FeatureRegistrySpec[Task]
    with ZioBaseSpec
    with JedisClientCreator {
  override def featureRegistry(): FeatureRegistry[Task] =
    runtime.unsafeRun(
      ZIO
        .service[Blocking.Service]
        .map(blocking => JedisZioFeatureRegistry.useClient(jedisPool, DefaultNamespace, blocking))
    )

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
