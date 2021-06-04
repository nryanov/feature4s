package feature4s

import feature4s.monad.MonadError

class InMemoryFeatureRegistrySpec extends BaseSpec {
  type Id[A] = A

  implicit val idMonadError = new MonadError[Id] {
    override def pure[A](value: A): Id[A] = value

    override def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

    override def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

    override def tap[A, B](fa: Id[A])(f: A => Id[B]): Id[A] = ???

    override def raiseError[A](error: Throwable): Id[A] = throw error

    override def mapError[A](fa: Id[A])(f: Throwable => Throwable): Id[A] =
      try fa
      catch {
        case err: Throwable => throw f(err)
      }

    override def ifM[A](fcond: Id[Boolean])(ifTrue: => Id[A], ifFalse: => Id[A]): Id[A] = ???

    override def whenA[A](cond: Boolean)(f: => Id[A]): Id[Unit] = ???

    override def void[A](fa: Id[A]): Id[Unit] = ???

    override def eval[A](f: => A): Id[A] = f

    override def guarantee[A](f: => Id[A])(g: => Id[Unit]): Id[A] = ???
  }

  test("test") {
    val registry = new InMemoryFeatureRegistry[Id]()

    val feature: Id[Feature[Id, String]] = registry.register("test", "1", None)

    println(feature.value())

    registry.update("test", "newValue")

    println(feature.value())

    registry.update("test", "newValue")

  }

  test("test 2") {
    val registry = new InMemoryFeatureRegistry[Id]()

    val feature: Id[Feature[Id, Int]] = registry.register("test", 1, None)

    println(feature.value())

    registry.update("test", "2")

    println(feature.value())

    registry.update("test", "newValue")

  }
}
