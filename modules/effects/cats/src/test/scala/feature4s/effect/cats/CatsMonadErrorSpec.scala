package feature4s.effect.cats

import cats.effect.IO
import feature4s.monad.{MonadError, MonadErrorSpec}

import scala.concurrent.Future

class CatsMonadErrorSpec extends MonadErrorSpec[IO] with CatsBaseSpec {
  override implicit val monadError: MonadError[IO] = new CatsMonadError[IO](blocker)

  override def toFuture[A](v: => IO[A]): Future[A] = v.unsafeToFuture()
}
