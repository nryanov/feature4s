package feature4s.monad

import feature4s.Id

object IdMonadError extends MonadError[Id] {
  override def pure[A](value: A): Id[A] = value

  override def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

  override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

  override def raiseError[A](error: Throwable): Id[A] = throw error

  override def mapError[A](fa: Id[A])(f: Throwable => Throwable): Id[A] =
    try fa
    catch {
      case e: Throwable => raiseError(f(e))
    }

  override def void[A](fa: Id[A]): Id[Unit] = unit

  override def eval[A](f: => A): Id[A] = f
}
