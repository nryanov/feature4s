package feature4s.zookeeper

import java.util.concurrent.TimeUnit

import feature4s.{FeatureRegistry, FeatureRegistrySpec, Id}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime

import scala.concurrent.Future
import scala.util.Try

class ZookeeperSyncFeatureRegistrySpec extends FeatureRegistrySpec[Id] with ZookeeperTestServer {
  private var zkClient: CuratorFramework = _

  override def featureRegistry(): FeatureRegistry[Id] = {
    val retryPolicy = new RetryOneTime(1000)
    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()
    client.blockUntilConnected(1, TimeUnit.SECONDS)

    zkClient = client
    ZookeeperSyncFeatureRegistry(client, "/features-node")
  }

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))

  override protected def afterEach(): Unit = {
    try zkClient.delete().deletingChildrenIfNeeded().forPath("/features-node")
    catch {
      case _: Throwable => // pass
    }
    zkClient.close()
    super.afterEach()
  }

}
