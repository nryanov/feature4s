package feature4s.effect.zio

import zio._
import feature4s.monad.{MonadError, MonadErrorSpec}

import scala.concurrent.Future

class ZioMonadAsyncErrorSpec extends MonadErrorSpec[Task] with ZioBaseSpec {
  override implicit val monadError: MonadError[Task] = new ZioMonadAsyncError()

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
