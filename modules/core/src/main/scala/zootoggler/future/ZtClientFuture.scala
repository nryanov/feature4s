package zootoggler.future

import zootoggler.core.configuration.ZtConfiguration
import zootoggler.core.{Attempt, Converter, FeatureAccessor, ZtClient, ZtClientBasic}

import scala.concurrent.{ExecutionContext, Future}

private final class ZtClientFuture(client: ZtClient[Attempt])(implicit ex: ExecutionContext)
    extends ZtClient[Future] {
  override def register[A: Converter](
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

  override def recreate[A: Converter](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Future[FeatureAccessor[Future, A]] =
    Future(client.recreate(defaultValue, name, description))
      .flatMap(result => toFuture(result))
      .map(accessor => featureAccessorAdapter(accessor))

  override def close(): Future[Unit] = Future(client.close()).flatMap(toFuture)

  private def toFuture[A](attempt: Attempt[A]): Future[A] = attempt match {
    case Attempt.Successful(value) => Future.successful(value)
    case Attempt.Failure(cause)    => Future.failed(cause)
  }

  private def featureAccessorAdapter[A: Converter](featureAccessor: FeatureAccessor[Attempt, A]) =
    new FeatureAccessor[Future, A] {
      override def value: Future[A] = Future(featureAccessor.value).flatMap(toFuture)

      override def update(newValue: A): Future[Boolean] =
        Future(featureAccessor.update(newValue)).flatMap(toFuture)
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
