package feature4s.local

import feature4s.Id
import feature4s.monad.IdMonadError

final class InMemoryIdFeatureRegistry extends InMemoryFeatureRegistry[Id](IdMonadError)

object InMemoryIdFeatureRegistry {
  def apply(): InMemoryIdFeatureRegistry = new InMemoryIdFeatureRegistry()
}
