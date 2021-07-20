package feature4s.monad

import feature4s.Id

import scala.concurrent.Future
import scala.util.Try

class IdMonadErrorSpec extends MonadErrorSpec[Id] {
  override implicit val monadError: MonadError[Id] = IdMonadError

  override def toFuture[A](v: => Id[A]): Future[A] = Future.fromTry(Try(v))
}
