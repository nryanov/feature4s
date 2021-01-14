package zootoggler.future

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.zookeeper.KeeperException.NodeExistsException
import org.slf4j.{Logger, LoggerFactory}
import zootoggler.core.Utils._
import zootoggler.core.configuration.{FeatureConfiguration, ZtConfiguration}
import zootoggler.core.{Attempt, Converter, Feature, ZtClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final class ZtClientFuture private (
  client: CuratorFramework,
  featureConfiguration: FeatureConfiguration
)(implicit ex: ExecutionContext)
    extends ZtClient[Future] {
  private val logger: Logger = LoggerFactory.getLogger(ZtClientFuture.getClass)

  private val path = featureConfiguration.rootPath

  private val namespace = featureConfiguration.namespace.getOrElse("")

  override def register[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Future[FeatureAccessor[A]] = {
    logger.debug(s"Register feature: $name")
    Future {
      Converter[A].toByteArray(defaultValue) match {
        case Attempt.Failure(cause) =>
          logger.error(
            s"Error happened while encoding default value: $defaultValue for feature: $name",
            cause
          )
          throw cause
        case Attempt.Successful(value) =>
          Try {
            client.createPath(fullPath(path, name), namespace, value)
          } match {
            case Success(_) =>
              logger.debug(s"Successfully register: $name")
              featureAccessor(Feature(defaultValue, name, description))
            case Failure(_: NodeExistsException) =>
              logger.debug(s"Feature: $name is already registered")
              featureAccessor(Feature(defaultValue, name, description))
            case Failure(exception) =>
              logger.error(s"Error while register feature: $name", exception)
              throw exception
          }
      }
    }
  }

  override def close(): Future[Unit] = Future(client.close())

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      override def value: Future[A] = Future {
        val data: Option[Array[Byte]] = client.get(feature.path(path), namespace)

        data match {
          case None        => throw new IllegalStateException(s"Feature does not exist: $feature")
          case Some(value) => Converter[A].fromByteArray(value).require
        }
      }

      override def update(newValue: A): Future[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          Future {
            client.set(feature.path(path), namespace, value)
          }.flatMap {
            case None =>
              Future.failed(new IllegalStateException(s"Feature does not exist: $feature"))
            case _ => Future.successful(())
          }
        case Attempt.Failure(cause) => Future.failed(cause)
      }
    }
}

object ZtClientFuture {
  def apply(client: CuratorFramework, featureConfiguration: FeatureConfiguration)(implicit
    ex: ExecutionContext
  ): ZtClient[Future] =
    new ZtClientFuture(client, featureConfiguration)(ex)

  def apply(cfg: ZtConfiguration, featureConfiguration: FeatureConfiguration)(implicit
    ex: ExecutionContext
  ): ZtClient[Future] = {
    val client = CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy)
    client.start()
    new ZtClientFuture(client, featureConfiguration)(ex)
  }
}
