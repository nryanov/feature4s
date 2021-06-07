package feature4s.monad

import scala.util.Try

object EitherMonadError extends MonadError[Either[Throwable, *]] {
  override def pure[A](value: A): Either[Throwable, A] = Right(value)

  override def map[A, B](fa: Either[Throwable, A])(f: A => B): Either[Throwable, B] = fa.map(f)

  override def flatMap[A, B](fa: Either[Throwable, A])(
    f: A => Either[Throwable, B]
  ): Either[Throwable, B] = fa.flatMap(f)

  override def raiseError[A](error: Throwable): Either[Throwable, A] = Left(error)

  override def mapError[A](
    fa: Either[Throwable, A]
  )(f: Throwable => Throwable): Either[Throwable, A] =
    fa match {
      case Left(value) => raiseError(f(value))
      case _           => fa
    }

  override def void[A](fa: Either[Throwable, A]): Either[Throwable, Unit] = fa.map(_ => ())

  override def eval[A](f: => A): Either[Throwable, A] = Try(f).toEither

}
