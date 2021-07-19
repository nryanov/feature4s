package feature4s.monad

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

class FutureMonadAsyncError(implicit ec: ExecutionContext) extends MonadAsyncError[Future] {
  override def pure[A](value: A): Future[A] = Future.successful(value)

  override def map[A, B](fa: => Future[A])(f: A => B): Future[B] = fa.map(f)

  override def flatMap[A, B](fa: => Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

  override def raiseError[A](error: Throwable): Future[A] = Future.failed(error)

  override def mapError[A](fa: => Future[A])(f: Throwable => Throwable): Future[A] =
    fa.transformWith {
      case Failure(exception) => raiseError(f(exception))
      case _                  => fa
    }

  override def handleErrorWith[A](fa: => Future[A])(
    pf: PartialFunction[Throwable, Future[A]]
  ): Future[A] = fa.recoverWith(pf)

  override def void[A](fa: => Future[A]): Future[Unit] = fa.map(_ => ())

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

  override def ifM[A](
    fcond: => Future[Boolean]
  )(ifTrue: => Future[A], ifFalse: => Future[A]): Future[A] =
    fcond.flatMap { flag =>
      if (flag) ifTrue
      else ifFalse
    }

  override def whenA[A](cond: Boolean)(f: => Future[A]): Future[Unit] =
    if (cond) f.map(_ => ())
    else unit

  override def guarantee[A](f: => Future[A])(g: => Future[Unit]): Future[A] = {
    val p = Promise[A]()

    suspend(f).onComplete {
      case Failure(exception) =>
        suspend(g).flatMap(_ => raiseError(exception)).onComplete(p.complete)
      case Success(value) => suspend(g).map(_ => value).onComplete(p.complete)
    }

    p.future
  }

  override def bracket[A, B](acquire: => Future[A])(use: A => Future[B])(
    release: A => Future[Unit]
  ): Future[B] = {
    val p = Promise[B]()

    suspend(acquire).onComplete {
      case Failure(exception) => p.failure(exception)
      case Success(resource) =>
        suspend(use(resource)).onComplete {
          case Failure(exception) =>
            suspend(release(resource)).flatMap(_ => raiseError(exception)).onComplete(p.complete)
          case Success(result) => suspend(release(resource)).map(_ => result).onComplete(p.complete)
        }
    }

    p.future
  }

  // sequential execution
  override def traverse[A, B](list: List[A])(f: A => Future[B]): Future[List[B]] =
    list.foldLeft(Future.successful(List.empty[B])) { case (state, a) =>
      for {
        s <- state
        b <- f(a)
      } yield b :: s
    }
}
