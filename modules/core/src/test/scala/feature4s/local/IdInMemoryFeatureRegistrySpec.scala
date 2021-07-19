package feature4s.local

import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}

import scala.concurrent.Future
import scala.util.Try

class IdInMemoryFeatureRegistrySpec extends FeatureRegistrySpec[Id] {
  override def featureRegistry(): FeatureRegistry[Id] = InMemoryIdFeatureRegistry()

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))
}
