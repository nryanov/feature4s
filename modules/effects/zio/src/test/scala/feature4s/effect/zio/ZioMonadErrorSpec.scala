package feature4s.effect.zio

import feature4s.monad.{MonadError, MonadErrorSpec}
import zio.blocking.Blocking
import zio.{Task, ZIO}

import scala.concurrent.Future

class ZioMonadErrorSpec extends MonadErrorSpec[Task] with ZioBaseSpec {
  override implicit val monadError: MonadError[Task] = runtime.unsafeRun(
    ZIO.service[Blocking.Service].map(blocking => new ZioMonadError(blocking))
  )

  override def toFuture[A](v: => Task[A]): Future[A] = runtime.unsafeRunToFuture(v)
}
