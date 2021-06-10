package feature4s.aerospike

import com.aerospike.client.AerospikeClient
import feature4s.Id
import feature4s.monad.IdMonadError

class AerospikeSyncFeatureRegistry private (client: AerospikeClient, namespace: String)
    extends AerospikeFeatureRegistry[Id](
      client = client,
      namespace = namespace,
      monad = IdMonadError
    )

object AerospikeSyncFeatureRegistry {
  def apply(client: AerospikeClient, namespace: String): AerospikeSyncFeatureRegistry =
    new AerospikeSyncFeatureRegistry(client, namespace)
}
