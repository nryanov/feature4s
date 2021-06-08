package feature4s.monad

import scala.util.{Failure, Success, Try}

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

  override def ifM[A](
    fcond: Either[Throwable, Boolean]
  )(ifTrue: => Either[Throwable, A], ifFalse: => Either[Throwable, A]): Either[Throwable, A] =
    fcond.flatMap { flag =>
      if (flag) ifTrue
      else ifFalse
    }

  override def whenA[A](cond: Boolean)(f: => Either[Throwable, A]): Either[Throwable, Unit] =
    if (cond) f.map(_ => ())
    else unit

  override def guarantee[A](
    f: => Either[Throwable, A]
  )(g: => Either[Throwable, Unit]): Either[Throwable, A] = {
    def tryE = Try(g) match {
      case Failure(exception) => Left(exception)
      case Success(value)     => value
    }

    // older scala versions are not supported, so we can assume that either is right-biased
    f match {
      case Left(value)  => tryE.flatMap(_ => Left(value))
      case Right(value) => tryE.map(_ => value)
    }
  }

  override def traverse[A, B](
    list: List[A]
  )(f: A => Either[Throwable, B]): Either[Throwable, List[B]] = {
    val initial: Either[Throwable, List[B]] = Right(List.empty)

    list.foldLeft(initial) { case (state, a) =>
      for {
        s <- state
        b <- f(a)
      } yield b :: s
    }
  }
}
