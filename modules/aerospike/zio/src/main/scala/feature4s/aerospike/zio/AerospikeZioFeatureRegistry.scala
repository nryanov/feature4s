package feature4s.aerospike.zio

import com.aerospike.client.AerospikeClient
import feature4s.aerospike.AerospikeFeatureRegistry
import feature4s.effect.zio.ZioMonadError
import zio.Task
import zio.blocking.Blocking

class AerospikeZioFeatureRegistry private (
  client: AerospikeClient,
  namespace: String,
  blocking: Blocking.Service
) extends AerospikeFeatureRegistry[Task](
      client = client,
      namespace = namespace,
      monad = new ZioMonadError(blocking)
    )

object AerospikeZioFeatureRegistry {
  def useClient(
    client: AerospikeClient,
    namespace: String,
    blocking: Blocking.Service
  ): AerospikeZioFeatureRegistry = new AerospikeZioFeatureRegistry(client, namespace, blocking)
}
