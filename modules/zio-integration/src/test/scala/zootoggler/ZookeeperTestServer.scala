package zootoggler

import zio.{Has, ZLayer, ZManaged}
import zio.blocking.{Blocking, effectBlocking}
import org.apache.curator.test.TestingServer

object ZookeeperTestServer {
  type Zookeeper = Has[TestingServer]

  def zookeeper(): ZLayer[Blocking, Throwable, Zookeeper] = ZManaged
    .make(
      effectBlocking {
        val server = new TestingServer(false)
        server.start()
        server
      }
    )(server => effectBlocking(server.stop()).orDie)
    .toLayer
}
