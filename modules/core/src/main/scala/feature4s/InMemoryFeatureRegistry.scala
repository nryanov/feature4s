package feature4s

import feature4s.monad.MonadError
import feature4s.monad.syntax._

import scala.collection.concurrent.TrieMap

final class InMemoryFeatureRegistry[F[_]](implicit val monad: MonadError[F])
    extends FeatureRegistry[F] {
  private val featureMeta: TrieMap[String, Feature[F, _]] = TrieMap()
  private val featureValues: TrieMap[String, String] = TrieMap()

  override def register[A](
    name: String,
    defaultValue: A,
    description: Option[String]
  )(implicit featureType: FeatureType[A]): F[Feature[F, A]] = for {
    feature <- monad.pure(
      Feature[F, A](
        name,
        () =>
          monad
            .eval(featureType.codec.decode(featureValues(name)))
            .flatMap {
              case Left(value)  => monad.raiseError[A](value)
              case Right(value) => monad.pure[A](value)
            }
            .mapError(_ => FeatureNotFound(name)),
        featureType,
        description
      )
    )
    value <- monad.eval(featureType.codec.encode(defaultValue))

    _ <- value match {
      case Left(error) => monad.raiseError(error)
      case Right(value) =>
        monad.eval(featureMeta.put(name, feature)) *> monad.eval(
          featureValues.put(name, value)
        )
    }
  } yield feature

  override def recreate[A](
    name: String,
    value: A,
    description: Option[String]
  )(implicit featureType: FeatureType[A]): F[Feature[F, A]] = ???

  override def update(name: String, value: String): F[Unit] = for {
    featureOpt <- monad.eval(featureMeta.get(name))
    _ <- featureOpt match {
      case Some(feature) =>
        for {
          result <- monad.eval(feature.featureType.codec.encodeFromString(value))
          _ <- result match {
            case Left(error)  => monad.raiseError(error)
            case Right(value) => monad.eval(featureValues.update(name, value))
          }
        } yield ()
      case None => monad.raiseError(FeatureNotFound(name))
    }

  } yield ()

  override def updateInfo(name: String, description: String): F[Unit] = ???

  override def featureList(): F[List[Feature[F, _]]] = monad.eval(featureMeta.values.toList)

  override def isExist(name: String): F[Boolean] = monad.eval(featureMeta.contains(name))

  override def remove(name: String): F[Boolean] = ???

  override def monadError: MonadError[F] = monad
}
