package feature4s.monad

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

class FutureMonadAsyncError(implicit ec: ExecutionContext) extends MonadAsyncError[Future] {
  override def pure[A](value: A): Future[A] = Future.successful(value)

  override def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

  override def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

  override def raiseError[A](error: Throwable): Future[A] = Future.failed(error)

  override def mapError[A](fa: Future[A])(f: Throwable => Throwable): Future[A] = fa.transformWith {
    case Failure(exception) => raiseError(f(exception))
    case _                  => fa
  }

  override def void[A](fa: Future[A]): Future[Unit] = fa.map(_ => ())

  override def eval[A](f: => A): Future[A] = Future(f)

  override def async[A](k: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
    val p = Promise[A]()

    k {
      case Left(value)  => p.failure(value)
      case Right(value) => p.success(value)
    }

    p.future
  }

  // ignore cancel logic
  override def cancelable[A](k: (Either[Throwable, A] => Unit) => () => Future[Unit]): Future[A] = {
    val p = Promise[A]()

    k {
      case Left(value)  => p.failure(value)
      case Right(value) => p.success(value)
    }

    p.future
  }
}
