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
    name: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] = {
    val path = s"$zNode/$name"
    val state = stateToBytes(enable, description)

    monad
      .handleErrorWith(
        monad
          .eval(client.create().creatingParentsIfNeeded().forPath(path, state))
          .void
          .map(_ => cacheMap.setState(name, enable, description))
      ) {
        case _: NodeExistsException => updateInfo(name, description)
        case err: Throwable         => monad.raiseError(ClientError(err))
      }
      .map(_ => Feature(name, cacheMap.featureAccessor(name), description))
  }

  override def recreate(
    name: String,
    enable: Boolean,
    description: Option[String]
  ): F[Feature[F]] = {
    val path = s"$zNode/$name"

    val state = stateToBytes(enable, description)

    monad
      .handleErrorWith(
        monad
          .eval(client.create().creatingParentsIfNeeded().forPath(path, state))
          .void
          .map(_ => cacheMap.setState(name, enable, description))
      ) {
        case _: NodeExistsException =>
          updateAll(name, enable, description)
        case err: Throwable => monad.raiseError(ClientError(err))
      }
      .map(_ => Feature(name, cacheMap.featureAccessor(name), description))
  }

  private def updateAll(name: String, enable: Boolean, description: Option[String]): F[Unit] = {
    val path = s"$zNode/$name"

    monad.mapError(
      monad
        .eval(
          client.setData().forPath(path, stateToBytes(enable, description))
        )
        .map(_ => cacheMap.setState(name, enable, description))
        .void
    )(ClientError)
  }

  private def updateInfo(name: String, description: Option[String]): F[Unit] = {
    val path = s"$zNode/$name"
    val stat = new Stat()

    monad.handleErrorWith(
      monad
        .eval(client.getData.storingStatIn(stat).forPath(path))
        .flatMap { data =>
          if (data == null) monad.raiseError[ZkState](FeatureNotFound(name))
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
            .map(_ => cacheMap.setInfo(name, description))
        }
        .void
    ) {
      // to handle concurrent updates
      case _: BadVersionException => updateInfo(name, description)
      case err: Throwable         => monad.raiseError(ClientError(err))
    }
  }

  override def update(name: String, enable: Boolean): F[Unit] = {
    val path = s"$zNode/$name"
    val stat = new Stat()

    monad.handleErrorWith(
      monad
        .eval(client.getData.storingStatIn(stat).forPath(path))
        .flatMap { data =>
          if (data == null) monad.raiseError[ZkState](FeatureNotFound(name))
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
        .map(_ => cacheMap.setEnable(name, enable))
        .void
    ) {
      // to handle concurrent updates
      case _: BadVersionException => update(name, enable)
      case _: NoNodeException     => monad.raiseError(FeatureNotFound(name))
      case err: Throwable         => monad.raiseError(ClientError(err))
    }
  }

  override def featureList(): F[List[FeatureState]] = monad.eval(cacheMap.featureList)

  override def isExist(name: String): F[Boolean] = {
    val path = s"$zNode/$name"
    monad.eval(client.checkExists().forPath(path)).map(stat => if (stat == null) false else true)
  }

  override def remove(name: String): F[Boolean] = {
    val path = s"$zNode/$name"
    monad.handleErrorWith(monad.eval(client.delete().guaranteed().forPath(path)).map(_ => true)) {
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
