package feature4s.local.zio

import feature4s.effect.zio.ZioMonadAsyncError
import feature4s.local.InMemoryFeatureRegistry
import zio.Task

final class InMemoryZioFeatureRegistry extends InMemoryFeatureRegistry[Task](new ZioMonadAsyncError) {}

object InMemoryZioFeatureRegistry {
  def apply(): InMemoryZioFeatureRegistry = new InMemoryZioFeatureRegistry()
}
