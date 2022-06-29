package feature4s.aerospike.cats

import cats.effect.Sync
import com.aerospike.client.AerospikeClient
import feature4s.aerospike.AerospikeFeatureRegistry
import feature4s.effect.cats.CatsMonadError

class AerospikeCatsFeatureRegistry[F[_]: ContextShift: Sync] private (
  client: AerospikeClient,
  namespace: String,
  blocker: Blocker
) extends AerospikeFeatureRegistry[F](
      client = client,
      namespace = namespace,
      monad = new CatsMonadError[F](blocker)
    )

object AerospikeCatsFeatureRegistry {
  def useClient[F[_]: ContextShift: Sync](
    client: AerospikeClient,
    namespace: String): AerospikeCatsFeatureRegistry[F] = new AerospikeCatsFeatureRegistry(client, namespace, blocker)
}
