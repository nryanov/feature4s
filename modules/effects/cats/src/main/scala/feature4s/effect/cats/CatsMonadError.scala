package feature4s.effect.cats

import cats.effect.{Blocker, ContextShift, Sync}
import feature4s.monad.MonadError

final class CatsMonadError[F[_]: ContextShift](blocker: Blocker)(implicit F: Sync[F])
    extends MonadError[F] {
  override def pure[A](value: A): F[A] = F.pure(value)

  override def map[A, B](fa: F[A])(f: A => B): F[B] = F.map(fa)(f)

  override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)

  override def raiseError[A](error: Throwable): F[A] = F.raiseError(error)

  override def mapError[A](fa: F[A])(f: Throwable => Throwable): F[A] =
    F.adaptError(fa) { case err: Throwable =>
      f(err)
    }

  override def void[A](fa: F[A]): F[Unit] = F.void(fa)

  override def eval[A](f: => A): F[A] = blocker.delay(f)

  override def unit: F[Unit] = F.unit
}
