package zootoggler.integration.zio

import zio.{Has, Task, ZIO, ZLayer, ZManaged}
import zio.blocking.Blocking
import zootoggler.core.Utils._
import zootoggler.core.{Attempt, Converter, Feature, ZtClient}
import zootoggler.core.configuration.{FeatureConfiguration, ZtConfiguration}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.zookeeper.KeeperException.NodeExistsException

final class ZtClientZio private (
  client: CuratorFramework,
  featureConfiguration: FeatureConfiguration,
  blocking: Blocking.Service
) extends ZtClient[Task]
    with Logging {

  private val path = featureConfiguration.rootPath

  private val namespace = featureConfiguration.namespace.getOrElse("")

  override def register[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Task[FeatureAccessor[A]] =
    createPath(defaultValue, name, description).map(feature => featureAccessor(feature))

  private def createPath[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Task[Feature[A]] = for {
    _ <- debug(s"Create new path: $path in namespace: $namespace")
    _ <- Converter[A].toByteArray(defaultValue) match {
      case Attempt.Successful(value) =>
        blocking
          .effectBlocking(
            client.createPath(fullPath(path, name), namespace, value)
          )
          .catchSome { case _: NodeExistsException =>
            info(s"Feature% $name is already registered")
          } *> info(
          s"Successfully register feature: $name"
        )
      case Attempt.Failure(cause) =>
        error(
          s"Error happened while encoding default feature value: $defaultValue for path: $path and namespace: $namespace",
          cause
        ) *> Task.fail(cause)
    }
  } yield Feature(defaultValue, name, description)

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      override def value: Task[A] = for {
        data <- blocking.effectBlocking(
          client.get(feature.path(path), namespace)
        )
        featureValue <- data match {
          case None =>
            error(s"Feature does not exist $feature") *> Task.fail(
              new IllegalStateException(s"Feature does not exist $feature")
            )
          case Some(value) =>
            Converter[A].fromByteArray(value) match {
              case Attempt.Successful(value) => Task.succeed(value)
              case Attempt.Failure(cause) =>
                error("Error happened while decoding actual value in") *> Task.fail(cause)
            }
        }
      } yield featureValue

      override def update(newValue: A): Task[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          for {
            result <- blocking.effectBlocking(
              client.set(feature.path(path), namespace, value)
            )
            _ <- Task.when(result.isEmpty)(
              error(s"Feature does not exist: $feature") *>
                Task.fail(new IllegalStateException(s"Feature does not exist: $feature"))
            )
          } yield ()
        case Attempt.Failure(cause) =>
          error(s"Error happened while encoding new value $newValue: $feature", cause) *> Task.fail(
            cause
          )
      }
    }

  def close(): Task[Unit] = Task.effect(client.close())
}

object ZtClientZio {
  type ZtClientEnv = Has[ZtClient[Task]]

  val live: ZLayer[Blocking with Has[ZtConfiguration] with Has[
    FeatureConfiguration
  ], Throwable, ZtClientEnv] =
    ZLayer.fromServicesManaged[
      Blocking.Service,
      ZtConfiguration,
      FeatureConfiguration,
      Any,
      Throwable,
      ZtClient[Task]
    ] { (blocking, cfg, featureCfg) =>
      make(blocking, cfg, featureCfg)
    }

  def make(
    blocking: Blocking.Service,
    cfg: ZtConfiguration,
    featureConfiguration: FeatureConfiguration
  ): ZManaged[Any, Throwable, ZtClient[Task]] =
    ZManaged.make(
      Task
        .effect(
          CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy)
        )
        .tap(client => Task.effect(client.start()))
        .map(new ZtClientZio(_, featureConfiguration, blocking))
    )(_.close().orDie)

  def register[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, ZtClient[Task]#FeatureAccessor[A]] =
    ZIO.accessM(_.get.register(defaultValue, name, description))

  def register[A: Converter](
    defaultValue: A,
    name: String
  ): ZIO[ZtClientEnv, Throwable, ZtClient[Task]#FeatureAccessor[A]] =
    ZIO.accessM(_.get.register(defaultValue, name, None))

  def close(): ZIO[ZtClientEnv, Throwable, Unit] = ZIO.accessM(_.get.close())
}
