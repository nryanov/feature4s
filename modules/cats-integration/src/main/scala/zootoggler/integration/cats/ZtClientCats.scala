package zootoggler.integration.cats

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import zootoggler.core.configuration.ZtConfiguration
import zootoggler.core.{Attempt, FeatureAccessor, FeatureType, FeatureView, ZtClient, ZtClientBasic}

final class ZtClientCats[F[_]: ContextShift] private (
  client: ZtClient[Attempt],
  blocker: Blocker
)(implicit F: Sync[F])
    extends ZtClient[F] {
  override def register[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[F, A]] =
    blocker
      .delay(client.register(defaultValue, name, description))
      .flatMap(result => toF(result))
      .map(accessor => featureAccessorAdapter(accessor))

  override def remove(name: String): F[Boolean] = blocker.delay(client.remove(name)).flatMap(toF)

  override def isExist(name: String): F[Boolean] = blocker.delay(client.isExist(name)).flatMap(toF)

  override def recreate[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): F[FeatureAccessor[F, A]] = blocker
    .delay(client.recreate(defaultValue, name, description))
    .flatMap(result => toF(result))
    .map(accessor => featureAccessorAdapter(accessor))

  override def update[A: FeatureType](
    name: String,
    newValue: A,
    description: Option[String]
  ): F[Boolean] =
    blocker.delay(client.update(name, newValue, description)).flatMap(toF)

  override def updateFromString(
    featureName: String,
    newValue: String,
    description: Option[String]
  ): F[Boolean] =
    blocker.delay(client.updateFromString(featureName, newValue, description)).flatMap(toF)

  override def updateFromByteArray(
    featureName: String,
    newValue: Array[Byte],
    description: Option[String]
  ): F[Boolean] =
    blocker.delay(client.updateFromByteArray(featureName, newValue, description)).flatMap(toF)

  override def featureList(): List[FeatureView] = client.featureList()

  override def close(): F[Unit] = blocker.delay(client.close()).flatMap(toF)

  private def featureAccessorAdapter[A: FeatureType](
    featureAccessor: FeatureAccessor[Attempt, A]
  ): FeatureAccessor[F, A] = new FeatureAccessor[F, A] {
    override def value: F[A] = blocker.delay(featureAccessor.value).flatMap(toF)
  }

  private def toF[A](attempt: Attempt[A]): F[A] = attempt match {
    case Attempt.Successful(value) => F.pure(value)
    case Attempt.Failure(cause)    => F.raiseError(cause)
  }
}

object ZtClientCats {
  def resource[F[_]: Sync: ContextShift](
    cfg: ZtConfiguration,
    blocker: Blocker
  ): Resource[F, ZtClient[F]] =
    Resource.make {
      Sync[F].delay(ZtClientBasic(cfg)).map(new ZtClientCats[F](_, blocker))
    }(_.close())

  def resource[F[_]: Sync: ContextShift](
    cfg: ZtConfiguration
  ): Resource[F, ZtClient[F]] = Blocker[F].flatMap(resource(cfg, _))
}
