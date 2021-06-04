//package zootoggler.core
//
//import scala.util.{Success, Try}
//
//sealed trait Attempt[+A] extends Product with Serializable {
//  def map[B](f: A => B): Attempt[B]
//
//  def mapErr(f: Throwable => Throwable): Attempt[A]
//
//  def flatMap[B](f: A => Attempt[B]): Attempt[B]
//
//  def flatten[B](implicit ev: A <:< Attempt[B]): Attempt[B]
//
//  def fold[B](failed: Throwable => B, succeed: A => B): B
//
//  /** failed and succeed should not throw any exceptions */
//  def tapBoth(failed: Throwable => Any, succeed: A => Any): Attempt[A]
//
//  def getOrElse[B >: A](default: => B): B
//
//  def orElse[B >: A](fallback: => Attempt[B]): Attempt[B]
//
//  def require: A
//
//  def isSuccessful: Boolean
//
//  def isFailure: Boolean = !isSuccessful
//
//  def toOption: Option[A]
//
//  def toEither: Either[Throwable, A]
//
//  def catchSome[B >: A](pf: PartialFunction[Throwable, Attempt[B]]): Attempt[B]
//}
//
//object Attempt {
//  def successful[A](value: A): Attempt[A] = Successful(value)
//
//  def failure[A](cause: Throwable): Attempt[A] = Failure(cause)
//
//  def fromOption[A](opt: Option[A]): Attempt[A] = opt match {
//    case Some(value) => successful(value)
//    case None        => failure(new NoSuchElementException("Option was empty"))
//  }
//
//  def delay[A](f: => A): Attempt[A] = Try(f) match {
//    case util.Failure(exception) => failure(exception)
//    case Success(value)          => successful(value)
//  }
//
//  final case class Successful[A](value: A) extends Attempt[A] { self =>
//    override def map[B](f: A => B): Attempt[B] = Successful(f(value))
//    override def mapErr(f: Throwable => Throwable): Attempt[A] = self
//    override def flatMap[B](f: A => Attempt[B]): Attempt[B] = f(value)
//    override def flatten[B](implicit ev: A <:< Attempt[B]): Attempt[B] = value
//    override def fold[B](failed: Throwable => B, succeed: A => B): B = succeed(value)
//    override def getOrElse[B >: A](default: => B): B = value
//    override def orElse[B >: A](fallback: => Attempt[B]): Attempt[B] = self
//    override def require: A = value
//    override def isSuccessful: Boolean = true
//    override def toOption: Option[A] = Some(value)
//    override def toEither: Either[Throwable, A] = Right(value)
//    override def tapBoth(failed: Throwable => Any, succeed: A => Any): Attempt[A] = {
//      succeed(value)
//      self
//    }
//    override def catchSome[B >: A](pf: PartialFunction[Throwable, Attempt[B]]): Attempt[B] = self
//  }
//
//  final case class Failure(cause: Throwable) extends Attempt[Nothing] { self =>
//    override def map[B](f: Nothing => B): Attempt[B] = self
//    override def mapErr(f: Throwable => Throwable): Attempt[Nothing] = Failure(f(cause))
//    override def flatMap[B](f: Nothing => Attempt[B]): Attempt[B] = self
//    override def flatten[B](implicit ev: Nothing <:< Attempt[B]): Attempt[B] = self
//    override def fold[B](failed: Throwable => B, succeed: Nothing => B): B = failed(cause)
//    override def getOrElse[B >: Nothing](default: => B): B = default
//    override def orElse[B >: Nothing](fallback: => Attempt[B]): Attempt[B] = fallback
//    override def require: Nothing = throw new IllegalStateException(cause)
//    override def isSuccessful: Boolean = false
//    override def toOption: Option[Nothing] = None
//    override def toEither: Either[Throwable, Nothing] = Left(cause)
//    override def tapBoth(failed: Throwable => Any, succeed: Nothing => Any): Attempt[Nothing] = {
//      failed(cause)
//      self
//    }
//    override def catchSome[B >: Nothing](pf: PartialFunction[Throwable, Attempt[B]]): Attempt[B] =
//      if (pf.isDefinedAt(cause)) {
//        pf.apply(cause)
//      } else {
//        self
//      }
//  }
//}
