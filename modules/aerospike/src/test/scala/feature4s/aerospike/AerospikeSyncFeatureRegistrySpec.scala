package feature4s.aerospike

import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}

import scala.concurrent.Future
import scala.util.Try

class AerospikeSyncFeatureRegistrySpec extends FeatureRegistrySpec[Id] with AerospikeClientCreator {
  override def featureRegistry(): FeatureRegistry[Id] =
    AerospikeSyncFeatureRegistry(aerospikeClient, "test")

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))
}
