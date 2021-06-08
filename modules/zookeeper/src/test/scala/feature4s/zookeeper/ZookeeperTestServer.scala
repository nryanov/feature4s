package feature4s.zookeeper

import org.apache.curator.test.TestingServer
import org.scalatest.{BeforeAndAfterAll, Suite}

trait ZookeeperTestServer extends BeforeAndAfterAll { self: Suite =>
  protected val server: TestingServer = new TestingServer(false)

  abstract override protected def beforeAll(): Unit = {
    server.start()
    super.beforeAll()
  }

  abstract override protected def afterAll(): Unit = {
    super.afterAll()
    server.stop()
  }
}
