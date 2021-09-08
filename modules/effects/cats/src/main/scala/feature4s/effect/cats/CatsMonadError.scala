package feature4s.effect.cats

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import feature4s.monad.MonadError

final class CatsMonadError[F[_]: ContextShift](blocker: Blocker)(implicit F: Sync[F])
    extends MonadError[F] {
  override def pure[A](value: A): F[A] = F.pure(value)

  override def map[A, B](fa: => F[A])(f: A => B): F[B] = F.map(fa)(f)

  override def flatMap[A, B](fa: => F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)

  override def raiseError[A](error: Throwable): F[A] = F.raiseError(error)

  override def mapError[A](fa: => F[A])(f: Throwable => Throwable): F[A] =
    F.adaptError(fa) { case err: Throwable =>
      f(err)
    }

  override def handleErrorWith[A](fa: => F[A])(pf: PartialFunction[Throwable, F[A]]): F[A] =
    F.handleErrorWith(fa)(pf)

  override def void[A](fa: => F[A]): F[Unit] = F.void(fa)

  override def eval[A](f: => A): F[A] = Sync[F].blocking(f)

  override def unit: F[Unit] = F.unit

  override def ifM[A](fcond: => F[Boolean])(ifTrue: => F[A], ifFalse: => F[A]): F[A] =
    F.ifM(fcond)(ifTrue, ifFalse)

  override def whenA[A](cond: Boolean)(f: => F[A]): F[Unit] =
    F.whenA(cond)(f)

  override def guarantee[A](f: => F[A])(g: => F[Unit]): F[A] = F.guarantee(f, g)

  override def bracket[A, B](acquire: => F[A])(use: A => F[B])(release: A => F[Unit]): F[B] =
    F.bracket(acquire)(use)(release)

  override def traverse[A, B](list: List[A])(f: A => F[B]): F[List[B]] =
    list.foldLeft(F.pure(List.empty[B])) { case (state, a) =>
      for {
        s <- state
        b <- f(a)
      } yield b :: s
    }
}
