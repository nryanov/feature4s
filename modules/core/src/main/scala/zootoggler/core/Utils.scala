package zootoggler.core

import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.data.Stat

private[zootoggler] object Utils {
  implicit class RichZkClient(val client: CuratorFramework) {
    def isExist(path: String, namespace: String): Boolean =
      Option(client.usingNamespace(namespace).checkExists().forPath(path)).isDefined

    def createPath(path: String, namespace: String): Unit =
      client.usingNamespace(namespace).create().creatingParentsIfNeeded().forPath(path)

    def createPath(path: String, namespace: String, value: Array[Byte]): Unit =
      client.usingNamespace(namespace).create().creatingParentsIfNeeded().forPath(path, value)

    def set(path: String, namespace: String, value: Array[Byte]): Option[Stat] =
      Option(client.usingNamespace(namespace).setData().forPath(path, value))

    def set(path: String, namespace: String, version: Int, value: Array[Byte]): Option[Stat] =
      Option(
        client.usingNamespace(namespace).setData().withVersion(version).forPath(path, value)
      )

    def get(path: String, namespace: String): Option[Array[Byte]] =
      Option(client.usingNamespace(namespace).getData().forPath(path))

    def getWithVersion(path: String, namespace: String): (Int, Array[Byte]) = {
      val stat: Stat = new Stat()
      (
        stat.getVersion,
        client.usingNamespace(namespace).getData().storingStatIn(stat).forPath(path)
      )
    }

    def delete(path: String, namespace: String): Unit =
      client.usingNamespace(namespace).delete().forPath(path)
  }

  implicit class RichFeature[A](val feature: Feature[A]) {
    def path(root: String): String = s"$root/${feature.name}"
  }

  def fullPath(root: String, name: String) = s"$root/$name"
}
