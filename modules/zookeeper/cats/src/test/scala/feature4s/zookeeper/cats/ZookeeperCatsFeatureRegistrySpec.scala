package feature4s.zookeeper.cats

import java.util.concurrent.TimeUnit

import cats.effect.IO
import feature4s.{FeatureRegistry, FeatureRegistrySpec}
import feature4s.effect.cats.CatsBaseSpec
import feature4s.zookeeper.ZookeeperTestServer
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime

import scala.concurrent.Future

class ZookeeperCatsFeatureRegistrySpec extends FeatureRegistrySpec[IO] with ZookeeperTestServer with CatsBaseSpec {
  private var zkClient: CuratorFramework = _

  override def featureRegistry(): FeatureRegistry[IO] = {
    val retryPolicy = new RetryOneTime(1000)
    val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
    client.start()
    client.blockUntilConnected(1, TimeUnit.SECONDS)

    zkClient = client

    ZookeeperCatsFeatureRegistry.useClient(client, "/features-node", blocker).unsafeRunSync()
  }

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()

  override protected def afterEach(): Unit = {
    try zkClient.delete().deletingChildrenIfNeeded().forPath("/features-node")
    catch {
      case _: Throwable => // pass
    }
    zkClient.close()
    super.afterEach()
  }
}
