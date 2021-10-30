package feature4s.zookeeper.zio

import java.util.concurrent.TimeUnit

import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.zio.ZioBaseSpec
import feature4s.zookeeper.ZookeeperTestServer
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime
import zio.Task

import scala.concurrent.Future

class ZookeeperZioFeatureRegistrySpec extends FeatureRegistrySpec[Task] with ZookeeperTestServer with ZioBaseSpec {
  private var zkClient: CuratorFramework = _

  override def featureRegistry(): FeatureRegistry[Task] = {
    val retryPolicy = new RetryOneTime(1000)
    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()
    client.blockUntilConnected(1, TimeUnit.SECONDS)

    zkClient = client

    runtime.unsafeRun(ZookeeperZioFeatureRegistry.useClient(client, "/features-node"))
  }

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)

  override protected def afterEach(): Unit = {
    try zkClient.delete().deletingChildrenIfNeeded().forPath("/features-node")
    catch {
      case _: Throwable => // pass
    }
    zkClient.close()
    super.afterEach()
  }
}
