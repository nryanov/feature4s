package feature4s.aerospike

import com.aerospike.client.AerospikeClient
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait AerospikeClientCreator
    extends TestContainerForAll
    with BeforeAndAfterAll
    with BeforeAndAfterEach { self: Suite =>
  override val containerDef: AerospikeContainer.Def = AerospikeContainer.Def()

  var aerospikeClient: AerospikeClient = _

  override def afterContainersStart(aerospike: AerospikeContainer): Unit =
    aerospikeClient = new AerospikeClient(aerospike.containerIpAddress, aerospike.mappedPort(3000))

  override protected def afterAll(): Unit = {
    aerospikeClient.close()
    super.afterAll()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    aerospikeClient.truncate(null, "test", null, null)
  }
}
