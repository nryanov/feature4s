package zootoggler.integration.cats

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import org.apache.zookeeper.KeeperException.NodeExistsException
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.apply._
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import zootoggler.core.Utils._
import zootoggler.core.configuration.{FeatureConfiguration, ZtConfiguration}
import zootoggler.core.{Attempt, Converter, Feature, ZtClient}

final class ZtClientCats[F[_]: ContextShift] private (
  client: CuratorFramework,
  featureConfiguration: FeatureConfiguration,
  blocker: Blocker
)(implicit F: Sync[F])
    extends ZtClient[F]
    with Logging[F] {

  private val path = featureConfiguration.rootPath

  private val namespace = featureConfiguration.namespace.getOrElse("")

  override def register[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[A]] =
    F.handleError(createPath(defaultValue, name)) {
      case _: NodeExistsException => info(s"Feature: $name is already registered")
      case other                  => raiseError(other)
    }.map(_ => featureAccessor(Feature(defaultValue, name, description)))

  override def close(): F[Unit] = blocker.delay(client.close())

  private def createPath[A: Converter](
    defaultValue: A,
    name: String
  ): F[Unit] = for {
    _ <- debug(s"Create new feature: $name")
    _ <- Converter[A].toByteArray(defaultValue) match {
      case Attempt.Successful(value) =>
        blocker.delay(
          client.createPath(fullPath(path, name), namespace, value)
        ) *> info(s"Successfully created new feature: $name")
      case Attempt.Failure(cause) =>
        error(
          s"Error happened while encoding default feature value: $defaultValue for path: $path and namespace: $namespace",
          cause
        ) *> raiseError(cause)
    }
  } yield ()

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      override def value: F[A] = for {
        data <- blocker.delay(client.get(feature.path(path), namespace))
        featureValue <- data match {
          case None =>
            error(s"Feature does not exist $feature") *> raiseError(
              new IllegalStateException(s"Feature does not exist $feature")
            )
          case Some(value) =>
            Converter[A].fromByteArray(value) match {
              case Attempt.Successful(value) => Sync[F].pure(value)
              case Attempt.Failure(cause) =>
                error("Error happened while decoding actual value in") *> raiseError(cause)
            }
        }
      } yield featureValue

      override def update(newValue: A): F[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          for {
            result <- blocker.delay(client.set(feature.path(path), namespace, value))
            _ <- F.whenA(result.isEmpty)(
              error(s"Feature does not exist: $feature") *>
                raiseError(new IllegalStateException(s"Feature does not exist: $feature"))
            )
          } yield ()
        case Attempt.Failure(cause) =>
          error(
            s"Error happened while encoding new value $newValue: $feature",
            cause
          ) *> raiseError(cause)
      }
    }

  private def raiseError[A](cause: Throwable): F[A] = F.raiseError(cause)
}

object ZtClientCats {
  def resource[F[_]: Sync: ContextShift](
    client: CuratorFramework,
    featureConfiguration: FeatureConfiguration,
    blocker: Blocker
  ): Resource[F, ZtClient[F]] =
    Resource
      .make(Sync[F].pure(new ZtClientCats[F](client, featureConfiguration, blocker)))(_.close())

  def resource[F[_]: Sync: ContextShift](
    client: CuratorFramework,
    featureConfiguration: FeatureConfiguration
  ): Resource[F, ZtClient[F]] =
    Blocker[F].flatMap(blocker => resource(client, featureConfiguration, blocker))

  def resource[F[_]: Sync: ContextShift](
    cfg: ZtConfiguration,
    featureConfiguration: FeatureConfiguration,
    blocker: Blocker
  ): Resource[F, ZtClient[F]] =
    Resource.make {
      Sync[F]
        .delay(CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy))
        .flatTap(client => Sync[F].delay(client.start()))
        .map(new ZtClientCats[F](_, featureConfiguration, blocker))
    }(_.close())

  def resource[F[_]: Sync: ContextShift](
    cfg: ZtConfiguration,
    featureConfiguration: FeatureConfiguration
  ): Resource[F, ZtClient[F]] = Blocker[F].flatMap(resource(cfg, featureConfiguration, _))
}
