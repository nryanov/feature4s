package feature4s.zookeeper

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch}

import feature4s.{ClientError, FeatureNotFound, FeatureState}
import feature4s.monad.MonadError
import feature4s.compat.CollectionConverters._
import feature4s.monad.syntax._
import feature4s.zookeeper.StateSerDe.stateFromBytes
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{ChildData, CuratorCacheListener}
import org.apache.zookeeper.KeeperException.NoNodeException

private[zookeeper] class FeatureCacheMap[F[_]](
  client: CuratorFramework,
  implicit val monad: MonadError[F],
  zNode: String,
  latch: CountDownLatch
) extends CuratorCacheListener {

  private val featureStates: ConcurrentHashMap[String, (Boolean, Option[String])] =
    new ConcurrentHashMap()

  def featureList: List[FeatureState] = featureStates.map {
    case (featureName, (isEnable, description)) => FeatureState(featureName, isEnable, description)
  }.toList

  def setEnable(featureName: String, flag: Boolean): Unit =
    featureStates.computeIfPresent(featureName, (_, current) => (flag, current._2))

  def setInfo(featureName: String, description: Option[String]): Unit =
    featureStates.computeIfPresent(featureName, (_, current) => (current._1, description))

  def setState(featureName: String, flag: Boolean, description: Option[String]): Unit =
    featureStates.put(featureName, (flag, description))

  def remove(featureName: String): Unit =
    featureStates.remove(featureName)

  override def event(
    eventType: CuratorCacheListener.Type,
    oldData: ChildData,
    data: ChildData
  ): Unit =
    eventType match {
      case CuratorCacheListener.Type.NODE_CHANGED =>
        if (pathContainsFeature(data.getPath)) {
          val path = data.getPath
          val featureState = data.getData
          val (isEnable, description) = stateFromBytes(featureState)

          featureStates.put(featureNamePath(path), (isEnable, description))
        }
      case CuratorCacheListener.Type.NODE_DELETED =>
        if (pathContainsFeature(oldData.getPath)) {
          val path = oldData.getPath
          featureStates.remove(featureNamePath(path))
        }
      case CuratorCacheListener.Type.NODE_CREATED =>
        if (pathContainsFeature(data.getPath)) {
          val path = data.getPath
          val featureState = data.getData
          val (isEnable, description) = stateFromBytes(featureState)

          featureStates.put(featureNamePath(path), (isEnable, description))
        }
    }

  override def initialized(): Unit = {
    latch.countDown()
    super.initialized()
  }

  def featureAccessor(featureName: String): () => F[Boolean] =
    () =>
      Option(featureStates.get(featureName)) match {
        case Some(value) => monad.pure(value._1)
        case None =>
          val path = s"$zNode/$featureName"
          // try to get value from zk
          monad.handleErrorWith(monad.eval(client.getData.forPath(path)).flatMap { data =>
            if (data == null) monad.raiseError[Boolean](FeatureNotFound(featureName))
            else monad.eval(stateFromBytes(data)._1)
          }) {
            case _: NoNodeException => monad.raiseError(FeatureNotFound(featureName))
            case err: Throwable     => monad.raiseError(ClientError(err))
          }
      }

  private def featureNamePath(path: String): String = path.substring(zNode.length + 1)

  private def pathContainsFeature(path: String): Boolean = path.length > zNode.length
}

object FeatureCacheMap {
  def apply[F[_]](
    client: CuratorFramework,
    monad: MonadError[F],
    zNode: String,
    latch: CountDownLatch
  ): FeatureCacheMap[F] = new FeatureCacheMap(client, monad, zNode, latch)
}
