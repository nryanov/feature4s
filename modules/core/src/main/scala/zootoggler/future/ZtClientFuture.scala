package zootoggler.future

import zootoggler.core.configuration.ZtConfiguration
import zootoggler.core.{Attempt, FeatureAccessor, FeatureType, FeatureView, ZtClient, ZtClientBasic}

import scala.concurrent.{ExecutionContext, Future}

private final class ZtClientFuture(client: ZtClient[Attempt])(implicit ex: ExecutionContext)
    extends ZtClient[Future] {
  override def register[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Future[FeatureAccessor[Future, A]] =
    Future(client.register(defaultValue, name, description))
      .flatMap(result => toFuture(result))
      .map(accessor => featureAccessorAdapter(accessor))

  override def remove(name: String): Future[Boolean] = Future(client.remove(name)).flatMap(toFuture)

  override def isExist(name: String): Future[Boolean] =
    Future(client.isExist(name)).flatMap(toFuture)

  override def recreate[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Future[FeatureAccessor[Future, A]] =
    Future(client.recreate(defaultValue, name, description))
      .flatMap(result => toFuture(result))
      .map(accessor => featureAccessorAdapter(accessor))

  override def close(): Future[Unit] = Future(client.close()).flatMap(toFuture)

  override def featureList(): List[FeatureView] = client.featureList()

  override def update[A: FeatureType](
    name: String,
    newValue: A,
    description: Option[String]
  ): Future[Boolean] =
    Future(client.update(name, newValue, description)).flatMap(toFuture)

  override def updateFromString(
    featureName: String,
    newValue: String,
    description: Option[String]
  ): Future[Boolean] =
    Future(client.updateFromString(featureName, newValue, description)).flatMap(toFuture)

  override def updateFromByteArray(
    featureName: String,
    newValue: Array[Byte],
    description: Option[String]
  ): Future[Boolean] =
    Future(client.updateFromByteArray(featureName, newValue, description)).flatMap(toFuture)

  private def toFuture[A](attempt: Attempt[A]): Future[A] = attempt match {
    case Attempt.Successful(value) => Future.successful(value)
    case Attempt.Failure(cause)    => Future.failed(cause)
  }

  private def featureAccessorAdapter[A: FeatureType](
    featureAccessor: FeatureAccessor[Attempt, A]
  ): FeatureAccessor[Future, A] =
    new FeatureAccessor[Future, A] {
      override def value: Future[A] = Future(featureAccessor.value).flatMap(toFuture)
    }
}

object ZtClientFuture {
  def apply(cfg: ZtConfiguration)(implicit
    ex: ExecutionContext
  ): ZtClient[Future] = {
    val inner: ZtClient[Attempt] = ZtClientBasic(cfg)
    new ZtClientFuture(inner)
  }
}
