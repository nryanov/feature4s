package zootoggler.integration.cats

import java.util.concurrent.atomic.AtomicReference

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import zootoggler.core.configuration.ZtConfiguration
import zootoggler.core.{Attempt, Converter, Feature, ZtClient}

final class ZtClientCats[F[_]: Sync: ContextShift] private (
  client: CuratorFramework,
  blocker: Blocker
) extends ZtClient[F]
    with Logging[F] {
  override def register[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): F[FeatureAccessor[A]] =
    Sync[F]
      .ifM(isPathExist(path, namespace))(
        readCurrentValue(path, namespace, description),
        createPath(defaultValue, path, namespace, description)
      )
      .map(feature => featureAccessor(feature))

  override def close(): F[Unit] = blocker.delay(client.close())

  private def isPathExist(path: String, namespace: Option[String]): F[Boolean] =
    blocker
      .delay(client.usingNamespace(namespace.getOrElse("")).checkExists().forPath(path))
      .map(_ != null)

  private def createPath[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): F[Feature[A]] = for {
    _ <- debug(s"Create new path: $path in namespace: $namespace")
    _ <- Converter[A].toByteArray(defaultValue) match {
      case Attempt.Successful(value) =>
        blocker.delay(
          client.usingNamespace(namespace.getOrElse("")).create().forPath(path, value)
        ) *> info(s"Successfully created new path: $path in namespace: $namespace")
      case Attempt.Failure(cause) =>
        error(
          s"Error happened while encoding default feature value: $defaultValue for path: $path and namespace: $namespace",
          cause
        ) *> raiseError(cause)
    }
  } yield Feature(defaultValue, path, namespace, description)

  private def readCurrentValue[A: Converter](
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): F[Feature[A]] = for {
    _ <- debug(s"Path: $path is already exist. Trying to get actual feature value")
    data <- blocker.delay(
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
        ) *> raiseError(cause)
    }
  } yield feature

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      private val cache = new AtomicReference[A](feature.value)

      override def cachedValue: A = cache.get()

      override def value: F[A] = for {
        data <- blocker.delay(
          client.usingNamespace(feature.namespace).getData().forPath(feature.path)
        )
        _ <- Sync[F].whenA(data == null)(
          error(s"Feature does not exist $feature") *> raiseError(
            new IllegalStateException(s"Feature does not exist $feature")
          )
        )
        feature <- Converter[A].fromByteArray(data) match {
          case Attempt.Successful(value) =>
            Sync[F].delay(cache.set(value)).map(_ => value)
          case Attempt.Failure(cause) =>
            error("Error happened while decoding actual value in") *> raiseError(cause)
        }
      } yield feature

      override def update(newValue: A): F[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          for {
            result <- blocker.delay(
              client.usingNamespace(feature.namespace).setData().forPath(feature.path, value)
            )
            _ <- Sync[F].whenA(result == null)(
              error(s"Feature does not exist: $feature") *>
                raiseError(new IllegalStateException(s"Feature does not exist: $feature"))
            )
            _ <- Sync[F].delay(cache.set(newValue))
          } yield ()
        case Attempt.Failure(cause) =>
          error(
            s"Error happened while encoding new value $newValue: $feature",
            cause
          ) *> raiseError(cause)
      }
    }

  private def raiseError[A](cause: Throwable): F[A] = Sync[F].raiseError(cause)
}

object ZtClientCats {
  def resource[F[_]: Sync: ContextShift](
    client: CuratorFramework,
    blocker: Blocker
  ): Resource[F, ZtClient[F]] =
    Resource.make(Sync[F].pure(new ZtClientCats[F](client, blocker)))(_.close())

  def resource[F[_]: Sync: ContextShift](
    client: CuratorFramework
  ): Resource[F, ZtClient[F]] = Blocker[F].flatMap(blocker =>
    Resource.make(Sync[F].pure(new ZtClientCats[F](client, blocker)))(_.close())
  )

  def resource[F[_]: Sync: ContextShift](
    cfg: ZtConfiguration,
    blocker: Blocker
  ): Resource[F, ZtClient[F]] =
    Resource.make {
      Sync[F]
        .delay(CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy))
        .flatTap(client => Sync[F].delay(client.start()))
        .map(new ZtClientCats[F](_, blocker))
    }(_.close())

  def resource[F[_]: Sync: ContextShift](
    cfg: ZtConfiguration
  ): Resource[F, ZtClient[F]] = Blocker[F].flatMap(resource(cfg, _))
}
