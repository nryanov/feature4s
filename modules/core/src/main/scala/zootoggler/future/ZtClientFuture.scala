package zootoggler.future

import java.util.concurrent.atomic.AtomicReference

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
    Future {
      if (client.usingNamespace(namespace.getOrElse("")).checkExists().forPath(path) == null) {
        logger.debug(s"Create new path: $path")
        Converter[A].toByteArray(defaultValue) match {
          case Attempt.Successful(value) =>
            client.usingNamespace(namespace.getOrElse("")).create().forPath(path, value)
            logger.debug(s"Successfully created new path: $path")
            featureAccessor(Feature(defaultValue, path, namespace, description))
          case Attempt.Failure(cause) =>
            logger.error(
              s"Error happened while encoding default feature value: $defaultValue for path: $path and namespace: $namespace",
              cause
            )
            throw cause
        }
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
  }

  override def close(): Future[Unit] = Future(client.close())

  private def featureAccessor[A: Converter](feature: Feature[A]): FeatureAccessor[A] =
    new FeatureAccessor[A] {
      private val cache = new AtomicReference[A](feature.value)

      override def cachedValue: A = cache.get()

      override def value: Future[A] = Future {
        val data: Array[Byte] =
          client.usingNamespace(feature.namespace).getData().forPath(feature.path)

        if (data == null) {
          throw new IllegalStateException(s"Feature does not exist: $feature")
        }

        Converter[A].fromByteArray(data) match {
          case Attempt.Successful(value) =>
            cache.set(value)
            value
          case Attempt.Failure(cause) => throw cause
        }
      }

      override def update(newValue: A): Future[Unit] = Converter[A].toByteArray(newValue) match {
        case Attempt.Successful(value) =>
          Future {
            client.usingNamespace(feature.namespace).setData().forPath(feature.path, value)
          }.flatMap {
            case null =>
              Future.failed(new IllegalStateException(s"Feature does not exist: $feature"))
            case _ =>
              cache.set(newValue)
              Future.successful(())
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
