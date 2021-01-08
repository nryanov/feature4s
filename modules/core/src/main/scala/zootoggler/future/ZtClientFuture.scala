package zootoggler.future

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.slf4j.{Logger, LoggerFactory}
import zootoggler.core.configuration.ZtConfiguration
import zootoggler.core.{Attempt, Converter, Feature, ZtClient}

import scala.concurrent.{ExecutionContext, Future}

final class ZtClientFuture private (client: CuratorFramework)(implicit ex: ExecutionContext)
    extends ZtClient[Future] {
  private val logger: Logger = LoggerFactory.getLogger(ZtClientFuture.getClass)

  override def register[A: Converter](
    defaultValue: A,
    path: String,
    namespace: Option[String],
    description: Option[String]
  ): Future[FeatureAccessor[A]] = {
    logger.debug(
      s"Register feature:\nDefault value: $defaultValue\nPath: $path,\nNamespace: $namespace,\nDescription: $description"
    )
    Converter[A].toByteArray(defaultValue) match {
      case Attempt.Successful(value) =>
        Future {
          if (client.usingNamespace(namespace.getOrElse("")).checkExists().forPath(path) == null) {
            logger.debug(s"Create new path: $path")
            client.usingNamespace(namespace.getOrElse("")).create().forPath(path, value)
            logger.debug(s"Successfully created new path: $path")
            featureAccessor(Feature(defaultValue, path, namespace, description))
          } else {
            logger.debug(s"Path: $path is already exist. Trying to get actual feature value")
            val data = client.usingNamespace(namespace.getOrElse("")).getData().forPath(path)
            Converter[A].fromByteArray(data) match {
              case Attempt.Successful(value) =>
                logger.debug(s"Successfully got actual value for path: $path")
                featureAccessor(Feature(value, path, namespace, description))
              case Attempt.Failure(cause) =>
                logger.error(s"Error happened while decoding actual value for path: $path", cause)
                throw cause
            }
          }
        }
      case Attempt.Failure(cause) =>
        logger.error(
          s"Error happened while encoding default feature value: $defaultValue for path: $path and namespace: $namespace",
          cause
        )
        Future.failed(cause)
    }
  }

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      override def value: Future[Feature[A]] = Future {
        val data: Array[Byte] =
          client.usingNamespace(feature.namespace).getData().forPath(feature.path)

        if (data == null) {
          throw new IllegalStateException(
            s"Feature does not exist for path: ${feature.path} in namespace: ${feature.namespace}"
          )
        }

        Converter[A].fromByteArray(data) match {
          case Attempt.Successful(value) => feature.copy(value = value)
          case Attempt.Failure(cause)    => throw cause
        }
      }

      override def update(newValue: A): Future[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          Future {
            client.usingNamespace(feature.namespace).setData().forPath(feature.path, value)
          }.flatMap {
            case null =>
              Future.failed(
                new IllegalStateException(
                  s"Feature does not exist for path: ${feature.path} in namespace: ${feature.namespace}"
                )
              )
            case _ => Future.successful(())
          }
        case Attempt.Failure(cause) => Future.failed(cause)
      }
    }
}

object ZtClientFuture {
  def apply(client: CuratorFramework)(implicit ex: ExecutionContext): ZtClient[Future] =
    new ZtClientFuture(client)(ex)

  def apply(cfg: ZtConfiguration)(implicit ex: ExecutionContext): ZtClient[Future] = {
    val client = CuratorFrameworkFactory.newClient(cfg.connectionString, cfg.retryPolicy)
    client.start()
    new ZtClientFuture(client)(ex)
  }
}
