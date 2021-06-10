package feature4s.monad

trait MonadError[F[_]] {
  def unit: F[Unit] = pure(())

  def pure[A](value: A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def raiseError[A](error: Throwable): F[A]

  def mapError[A](fa: F[A])(f: Throwable => Throwable): F[A]

  def handleErrorWith[A](fa: => F[A])(pf: PartialFunction[Throwable, F[A]]): F[A]

  def ifM[A](fcond: F[Boolean])(ifTrue: => F[A], ifFalse: => F[A]): F[A]

  def whenA[A](cond: Boolean)(f: => F[A]): F[Unit]

  def void[A](fa: F[A]): F[Unit]

  def eval[A](f: => A): F[A]

  def guarantee[A](f: => F[A])(g: => F[Unit]): F[A]

  def traverse[A, B](list: List[A])(f: A => F[B]): F[List[B]]

  def suspend[A](fa: => F[A]): F[A] = flatten(eval(fa))

  def flatten[A](fa: F[F[A]]): F[A] = flatMap(fa)(v => identity(v))
}
