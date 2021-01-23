package zootoggler.integration.zio

import zio.{Has, Task, ZIO, ZLayer, ZManaged}
import zio.blocking.Blocking
import zootoggler.core.{Attempt, FeatureAccessor, FeatureType, FeatureView, ZtClient, ZtClientBasic}
import zootoggler.core.configuration.ZtConfiguration

final class ZtClientZio private (
  client: ZtClient[Attempt],
  blocking: Blocking.Service
) extends ZtClient[Task] {
  override def register[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Task[FeatureAccessor[Task, A]] =
    blocking
      .effectBlocking(client.register(defaultValue, name, description))
      .flatMap(result => toTask(result))
      .map(accessor => featureAccessorAdapter(accessor))

  override def remove(name: String): Task[Boolean] =
    blocking.effectBlocking(client.remove(name)).flatMap(toTask)

  override def isExist(name: String): Task[Boolean] =
    blocking.effectBlocking(client.isExist(name)).flatMap(toTask)

  override def recreate[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): Task[FeatureAccessor[Task, A]] = blocking
    .effectBlocking(client.recreate(defaultValue, name, description))
    .flatMap(result => toTask(result))
    .map(accessor => featureAccessorAdapter(accessor))

  override def update[A: FeatureType](
    name: String,
    newValue: A,
    description: Option[String]
  ): Task[Boolean] =
    blocking.effectBlocking(client.update(name, newValue, description)).flatMap(toTask)

  override def updateFromString(
    featureName: String,
    newValue: String,
    description: Option[String]
  ): Task[Boolean] =
    blocking
      .effectBlocking(client.updateFromString(featureName, newValue, description))
      .flatMap(toTask)

  override def updateFromByteArray(
    featureName: String,
    newValue: Array[Byte],
    description: Option[String]
  ): Task[Boolean] = blocking
    .effectBlocking(client.updateFromByteArray(featureName, newValue, description))
    .flatMap(toTask)

  override def featureList(): List[FeatureView] = client.featureList()

  override def close(): Task[Unit] = blocking.effectBlocking(client.close()).flatMap(toTask)

  private def featureAccessorAdapter[A: FeatureType](
    featureAccessor: FeatureAccessor[Attempt, A]
  ): FeatureAccessor[Task, A] = new FeatureAccessor[Task, A] {
    override def value: Task[A] = blocking.effectBlocking(featureAccessor.value).flatMap(toTask)
  }

  private def toTask[A](attempt: Attempt[A]): Task[A] = attempt match {
    case Attempt.Successful(value) => Task.succeed(value)
    case Attempt.Failure(cause)    => Task.fail(cause)
  }
}

object ZtClientZio {
  type ZtClientEnv = Has[ZtClient[Task]]

  val live: ZLayer[Blocking with Has[ZtConfiguration], Throwable, ZtClientEnv] =
    ZLayer.fromServicesManaged[
      Blocking.Service,
      ZtConfiguration,
      Any,
      Throwable,
      ZtClient[Task]
    ] { (blocking, cfg) =>
      make(blocking, cfg)
    }

  def make(
    blocking: Blocking.Service,
    cfg: ZtConfiguration
  ): ZManaged[Any, Throwable, ZtClient[Task]] =
    ZManaged.make(
      Task.effect(ZtClientBasic(cfg)).map(new ZtClientZio(_, blocking))
    )(_.close().orDie)

  def register[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, FeatureAccessor[Task, A]] =
    ZIO.accessM(_.get.register(defaultValue, name, description))

  def register[A: FeatureType](
    defaultValue: A,
    name: String
  ): ZIO[ZtClientEnv, Throwable, FeatureAccessor[Task, A]] =
    ZIO.accessM(_.get.register(defaultValue, name, None))

  def recreate[A: FeatureType](
    defaultValue: A,
    name: String,
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, FeatureAccessor[Task, A]] =
    ZIO.accessM(_.get.recreate(defaultValue, name, description))

  def recreate[A: FeatureType](
    defaultValue: A,
    name: String
  ): ZIO[ZtClientEnv, Throwable, FeatureAccessor[Task, A]] =
    ZIO.accessM(_.get.recreate(defaultValue, name, None))

  def update[A: FeatureType](
    name: String,
    newValue: A,
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, Boolean] =
    ZIO.accessM(_.get.update(name, newValue, description))

  def update[A: FeatureType](name: String, newValue: A): ZIO[ZtClientEnv, Throwable, Boolean] =
    ZIO.accessM(_.get.update(name, newValue))

  def updateFromString(
    name: String,
    newValue: String,
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, Boolean] =
    ZIO.accessM(_.get.updateFromString(name, newValue, description))

  def updateFromString(name: String, newValue: String): ZIO[ZtClientEnv, Throwable, Boolean] =
    ZIO.accessM(_.get.updateFromString(name, newValue))

  def updateFromByteArray(
    name: String,
    newValue: Array[Byte],
    description: Option[String]
  ): ZIO[ZtClientEnv, Throwable, Boolean] =
    ZIO.accessM(_.get.updateFromByteArray(name, newValue, description))

  def updateFromByteArray(
    name: String,
    newValue: Array[Byte]
  ): ZIO[ZtClientEnv, Throwable, Boolean] =
    ZIO.accessM(_.get.updateFromByteArray(name, newValue))

  def remove(name: String): ZIO[ZtClientEnv, Throwable, Boolean] = ZIO.accessM(_.get.remove(name))

  def isExist(name: String): ZIO[ZtClientEnv, Throwable, Boolean] = ZIO.accessM(_.get.isExist(name))

  def featureList(): ZIO[ZtClientEnv, Throwable, List[FeatureView]] =
    ZIO.access(_.get.featureList())

  def close(): ZIO[ZtClientEnv, Throwable, Unit] = ZIO.accessM(_.get.close())
}
