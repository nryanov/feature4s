package feature4s.redis.lettuce.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import feature4s.redis.DefaultNamespace
import feature4s.redis.lettuce.LettuceClientCreator
import zio._
import zio.blocking.Blocking

import scala.concurrent.Future

class LettuceZioFeatureRegistrySpec
    extends FeatureRegistrySpec[Task]
    with LettuceClientCreator
    with ZioBaseSpec {
  override def featureRegistry(): FeatureRegistry[Task] =
    runtime.unsafeRun(
      ZIO
        .service[Blocking.Service]
        .map(blocking =>
          LettuceZioFeatureRegistry.useConnection(redisClient.connect(), DefaultNamespace, blocking)
        )
    )

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
