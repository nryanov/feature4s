package feature4s.aerospike.zio

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.aerospike.AerospikeClientCreator
import feature4s.effect.zio.ZioBaseSpec
import zio.blocking.Blocking
import zio.{Task, ZIO}

import scala.concurrent.Future

class AerospikeZioFeatureRegistrySpec
    extends FeatureRegistrySpec[Task]
    with AerospikeClientCreator
    with ZioBaseSpec {
  override def featureRegistry(): FeatureRegistry[Task] = runtime.unsafeRun(
    ZIO
      .service[Blocking.Service]
      .map(blocking => AerospikeZioFeatureRegistry.useClient(aerospikeClient, "test", blocking))
  )

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
