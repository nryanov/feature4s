package feature4s.local.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import zio.Task

import scala.concurrent.Future

class InMemoryZioFeatureRegistrySpec extends FeatureRegistrySpec[Task] with ZioBaseSpec {
  override def featureRegistry(): FeatureRegistry[Task] = InMemoryZioFeatureRegistry()

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
