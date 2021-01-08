package zootoggler.core

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.api.ErrorListenerPathAndBytesable
import org.apache.curator.retry.ExponentialBackoffRetry
import zootoggler.ZkTestServer

class ZtClientSpec extends ZkTestServer {
  "ZkClient" should {
    "do something" in {
      val retryPolicy = new ExponentialBackoffRetry(1000, 5)
      val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)

      client.start()
      client.create().forPath("/test")
      client.setData().forPath("/test", Array[Byte](1, 2, 3, 4, 5))
      val a: Array[Byte] = client.getData.forPath("/test")

      println(a.toList)
    }
  }
}
