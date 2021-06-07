package feature4s.monad

trait MonadError[F[_]] {
  def unit: F[Unit] = pure(())

  def pure[A](value: A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def raiseError[A](error: Throwable): F[A]

  def mapError[A](fa: F[A])(f: Throwable => Throwable): F[A]

  def void[A](fa: F[A]): F[Unit]

  def eval[A](f: => A): F[A]
}
