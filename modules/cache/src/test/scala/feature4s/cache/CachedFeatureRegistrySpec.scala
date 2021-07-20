package feature4s.cache

import feature4s.local.InMemoryIdFeatureRegistry
import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Try

class CachedFeatureRegistrySpec extends FeatureRegistrySpec[Id] {
  override def featureRegistry(): FeatureRegistry[Id] =
    CachedFeatureRegistry.wrap(InMemoryIdFeatureRegistry(), 5.minutes)

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))
}
