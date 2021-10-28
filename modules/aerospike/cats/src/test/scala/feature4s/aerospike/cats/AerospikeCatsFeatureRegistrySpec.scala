package feature4s.aerospike.cats

import cats.effect.IO
import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.aerospike.AerospikeClientCreator
import feature4s.effect.cats.CatsBaseSpec

import scala.concurrent.Future

class AerospikeCatsFeatureRegistrySpec extends FeatureRegistrySpec[IO] with AerospikeClientCreator with CatsBaseSpec {
  override def featureRegistry(): FeatureRegistry[IO] =
    AerospikeCatsFeatureRegistry.useClient(aerospikeClient, "test", blocker)

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()
}
