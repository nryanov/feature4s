package zootoggler.integration.zio

import java.util.concurrent.atomic.AtomicReference

import zio.{Has, Task, ZIO, ZLayer, ZManaged}
import zio.blocking.Blocking
import zootoggler.core.configuration.ZtConfiguration
import zootoggler.core.{Attempt, Converter, Feature, ZtClient}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}

final class ZtClientZio private (client: CuratorFramework, blocking: Blocking.Service)
    extends ZtClient[Task]
    with Logging {
  override def register[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): Task[FeatureAccessor[A]] =
    Task
      .ifM(isPathExist(path, namespace))(
        readCurrentValue(path, namespace, description),
        createPath(defaultValue, path, namespace, description)
      )
      .map(feature => featureAccessor(feature))

  private def isPathExist(path: String, namespace: Option[String]): Task[Boolean] =
    blocking
      .effectBlocking(client.usingNamespace(namespace.getOrElse("")).checkExists().forPath(path))
      .map(_ != null)

  private def createPath[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): Task[Feature[A]] = for {
    _ <- debug(s"Create new path: $path in namespace: $namespace")
    _ <- Converter[A].toByteArray(defaultValue) match {
      case Attempt.Successful(value) =>
        blocking.effectBlocking(
          client.usingNamespace(namespace.getOrElse("")).create().forPath(path, value)
        ) *> info(
          s"Successfully created new path: $path in namespace: $namespace"
        )
      case Attempt.Failure(cause) =>
        error(
          s"Error happened while encoding default feature value: $defaultValue for path: $path and namespace: $namespace",
          cause
        ) *> Task.fail(cause)
    }
  } yield Feature(defaultValue, path, namespace, description)

  private def readCurrentValue[A: Converter](
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): Task[Feature[A]] = for {
    _ <- debug(s"Path: $path is already exist. Trying to get actual feature value")
    data <- blocking.effectBlocking(
      client.usingNamespace(namespace.getOrElse("")).getData().forPath(path)
    )
    feature <- Converter[A].fromByteArray(data) match {
      case Attempt.Successful(value) =>
        debug(s"Successfully got actual value for path: $path in namespace: $namespace").map(_ =>
          Feature(value, path, namespace, description)
        )
      case Attempt.Failure(cause) =>
        error(
          s"Error happened while decoding actual value for path: $path in namespace: $namespace",
          cause
        ) *> Task.fail(cause)
    }
  } yield feature

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      private val cache = new AtomicReference[A](feature.value)

      override def cachedValue: A = cache.get()

      override def value: Task[A] = for {
        data <- blocking.effectBlocking(
          client.usingNamespace(feature.namespace).getData().forPath(feature.path)
        )
        _ <- Task.when(data == null)(
          error(s"Feature does not exist $feature") *> Task.fail(
            new IllegalStateException(s"Feature does not exist $feature")
          )
        )
        feature <- Converter[A].fromByteArray(data) match {
          case Attempt.Successful(value) =>
            Task.effectTotal(cache.set(value)).map(_ => value)
          case Attempt.Failure(cause) =>
            error("Error happened while decoding actual value in") *> Task.fail(cause)
        }
      } yield feature

      override def update(newValue: A): Task[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          for {
            result <- blocking.effectBlocking(
              client.usingNamespace(feature.namespace).setData().forPath(feature.path, value)
            )
            _ <- Task.when(result == null)(
              error(s"Feature does not exist: $feature") *>
                Task.fail(new IllegalStateException(s"Feature does not exist: $feature"))
            )
            _ <- Task.effectTotal(cache.set(newValue))
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

  val live: ZLayer[Blocking with Has[ZtConfiguration], Throwable, ZtClientEnv] =
    ZLayer.fromServicesManaged[Blocking.Service, ZtConfiguration, Any, Throwable, ZtClient[Task]] {
      (blocking, cfg) => make(blocking, cfg)
    }

  def make(
    blocking: Blocking.Service,
    cfg: ZtConfiguration
  ): ZManaged[Any, Throwable, ZtClient[Task]] =
    ZManaged.make(
      Task
        .effect(
          CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy)
        )
        .tap(client => Task.effect(client.start()))
        .map(new ZtClientZio(_, blocking))
    )(_.close().orDie)

  def register[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, ZtClient[Task]#FeatureAccessor[A]] =
    ZIO.accessM(_.get.register(defaultValue, path, namespace, description))

  def register[A: Converter](
    defaultValue: A,
    path: String
  ): ZIO[ZtClientEnv, Throwable, ZtClient[Task]#FeatureAccessor[A]] =
    ZIO.accessM(_.get.register(defaultValue, path, None, None))

  def close(): ZIO[ZtClientEnv, Throwable, Unit] = ZIO.accessM(_.get.close())
}
