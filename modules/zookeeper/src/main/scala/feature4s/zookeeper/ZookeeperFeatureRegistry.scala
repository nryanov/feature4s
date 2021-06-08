package feature4s.zookeeper

import feature4s.{Feature, FeatureRegistry, FeatureState}
import feature4s.monad.MonadError
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.CuratorCache

abstract class ZookeeperFeatureRegistry[F[_]](
  client: CuratorFramework,
  cache: CuratorCache,
  implicit val monad: MonadError[F]
) extends FeatureRegistry[F] {

  override def register(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    ???

  override def recreate(name: String, enable: Boolean, description: Option[String]): F[Feature[F]] =
    ???

  override def update(name: String, enable: Boolean): F[Unit] = ???

  override def updateInfo(name: String, description: String): F[Unit] = ???

  override def featureList(): F[List[FeatureState]] = ???

  override def isExist(name: String): F[Boolean] = ???

  override def remove(name: String): F[Boolean] = ???

  override def close(): F[Unit] = ???

  override def monadError: MonadError[F] = ???
}
