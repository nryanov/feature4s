package feature4s.zookeeper

import feature4s.{ClientError, Feature, FeatureNotFound, FeatureRegistry, FeatureState}
import feature4s.monad.MonadError
import feature4s.monad.syntax._
import feature4s.zookeeper.StateSerDe.{stateFromBytes, stateToBytes}
import feature4s.zookeeper.ZookeeperFeatureRegistry.ZkState
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.CuratorCache
import org.apache.zookeeper.KeeperException.{
  BadVersionException,
  NoNodeException,
  NodeExistsException
}
import org.apache.zookeeper.data.Stat

abstract class ZookeeperFeatureRegistry[F[_]](
  client: CuratorFramework,
  cache: CuratorCache,
  cacheMap: FeatureCacheMap[F],
  zNode: String,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {

  override def register(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] = {
    val path = s"$zNode/$featureName"
    val state = stateToBytes(enable, description)

    monad
      .handleErrorWith(
        monad
          .eval(client.create().creatingParentsIfNeeded().forPath(path, state))
          .void
          .map(_ => cacheMap.setState(featureName, enable, description))
      ) {
        case _: NodeExistsException => updateInfo(featureName, description)
        case err: Throwable         => monad.raiseError(ClientError(err))
      }
      .map(_ => Feature(featureName, cacheMap.featureAccessor(featureName), description))
  }

  override def recreate(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] = {
    val path = s"$zNode/$featureName"

    val state = stateToBytes(enable, description)

    monad
      .handleErrorWith(
        monad
          .eval(client.create().creatingParentsIfNeeded().forPath(path, state))
          .void
          .map(_ => cacheMap.setState(featureName, enable, description))
      ) {
        case _: NodeExistsException =>
          updateAll(featureName, enable, description)
        case err: Throwable => monad.raiseError(ClientError(err))
      }
      .map(_ => Feature(featureName, cacheMap.featureAccessor(featureName), description))
  }

  private def updateAll(
    featureName: String,
    enable: Boolean,
    description: Option[String]
  ): F[Unit] = {
    val path = s"$zNode/$featureName"

    monad.mapError(
      monad
        .eval(
          client.setData().forPath(path, stateToBytes(enable, description))
        )
        .map(_ => cacheMap.setState(featureName, enable, description))
        .void
    )(ClientError)
  }

  private def updateInfo(featureName: String, description: Option[String]): F[Unit] = {
    val path = s"$zNode/$featureName"
    val stat = new Stat()

    monad.handleErrorWith(
      monad
        .eval(client.getData.storingStatIn(stat).forPath(path))
        .flatMap { data =>
          if (data == null) monad.raiseError[ZkState](FeatureNotFound(featureName))
          else monad.pure(ZkState(stat.getVersion, data))
        }
        .flatMap { state =>
          monad
            .eval(
              client
                .setData()
                .withVersion(state.version)
                .forPath(path, stateToBytes(stateFromBytes(state.data)._1, description))
            )
            .map(_ => cacheMap.setInfo(featureName, description))
        }
        .void
    ) {
      // to handle concurrent updates
      case _: BadVersionException => updateInfo(featureName, description)
      case err: Throwable         => monad.raiseError(ClientError(err))
    }
  }

  override def update(featureName: String, enable: Boolean): F[Unit] = {
    val path = s"$zNode/$featureName"
    val stat = new Stat()

    monad.handleErrorWith(
      monad
        .eval(client.getData.storingStatIn(stat).forPath(path))
        .flatMap { data =>
          if (data == null) monad.raiseError[ZkState](FeatureNotFound(featureName))
          else monad.pure(ZkState(stat.getVersion, data))
        }
        .flatMap { state =>
          monad.eval(
            client
              .setData()
              .withVersion(state.version)
              .forPath(path, stateToBytes(enable, stateFromBytes(state.data)._2))
          )
        }
        .map(_ => cacheMap.setEnable(featureName, enable))
        .void
    ) {
      // to handle concurrent updates
      case _: BadVersionException => update(featureName, enable)
      case _: NoNodeException     => monad.raiseError(FeatureNotFound(featureName))
      case err: Throwable         => monad.raiseError(ClientError(err))
    }
  }

  override def featureList(): F[List[FeatureState]] = monad.eval(cacheMap.featureList)

  override def isExist(featureName: String): F[Boolean] = {
    val path = s"$zNode/$featureName"
    monad.eval(client.checkExists().forPath(path)).map(stat => if (stat == null) false else true)
  }

  override def remove(featureName: String): F[Boolean] = {
    val path = s"$zNode/$featureName"
    monad.handleErrorWith(
      monad
        .eval(client.delete().guaranteed().forPath(path))
        .map(_ => true)
        .flatMap(result => monad.eval(cacheMap.remove(featureName)).map(_ => result))
    ) {
      case _: NoNodeException => monad.pure(false)
      case err: Throwable     => monad.raiseError(err)
    }
  }

  override def close(): F[Unit] = monad.eval(cache.close())

  override def monadError: MonadError[F] = monad
}

object ZookeeperFeatureRegistry {
  final case class ZkState(version: Int, data: Array[Byte])
}
