package zootoggler

import org.apache.curator.test.TestingServer
import org.scalatest.BeforeAndAfterAll

trait ZkTestServer extends BaseSpec with BeforeAndAfterAll {
  protected val server: TestingServer = new TestingServer(false)

  override protected def beforeAll(): Unit =
    server.start()

  override protected def afterAll(): Unit =
    server.stop()
}
