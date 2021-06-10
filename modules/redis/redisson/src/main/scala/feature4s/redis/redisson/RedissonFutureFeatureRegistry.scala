package feature4s.redis.redisson

import feature4s.monad.FutureMonadAsyncError
import org.redisson.api.RedissonClient

import scala.concurrent.{ExecutionContext, Future}

class RedissonFutureFeatureRegistry private (client: RedissonClient, namespace: String)(implicit
  val ex: ExecutionContext
) extends RedissonFeatureRegistry[Future](
      client = client,
      namespace = namespace,
      monad = new FutureMonadAsyncError()
    )

object RedissonFutureFeatureRegistry {
  def apply(client: RedissonClient, namespace: String)(implicit
    ex: ExecutionContext
  ): RedissonFutureFeatureRegistry = new RedissonFutureFeatureRegistry(client, namespace)(ex)
}
