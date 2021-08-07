package feature4s.local.cats

import cats.effect.IO
import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.cats.CatsBaseSpec

import scala.concurrent.Future

class InMemoryCatsFeatureRegistrySpec extends FeatureRegistrySpec[IO] with CatsBaseSpec {
  override def featureRegistry(): FeatureRegistry[IO] = InMemoryCatsFeatureRegistry()

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()
}
