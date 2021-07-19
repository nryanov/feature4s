package feature4s.local.cats

import cats.effect.Concurrent
import feature4s.effect.cats.CatsMonadAsyncError
import feature4s.local.InMemoryFeatureRegistry

final class InMemoryCatsFeatureRegistry[F[_]: Concurrent]
    extends InMemoryFeatureRegistry[F](new CatsMonadAsyncError[F]())

object InMemoryCatsFeatureRegistry {
  def apply[F[_]: Concurrent](): InMemoryCatsFeatureRegistry[F] = new InMemoryCatsFeatureRegistry()
}
