//package zootoggler.core
//
//import java.nio.ByteBuffer
//import java.nio.charset.StandardCharsets
//
//import zootoggler.core.Attempt.{Failure, Successful}
//
//import scala.util.{Success, Try}
//
//trait Converter[A] { self =>
//  def toByteArray(value: A): Attempt[Array[Byte]]
//
//  def fromByteArray(value: Array[Byte]): Attempt[A]
//
//  final def fromBuffer(value: ByteBuffer): Attempt[A] = fromByteArray(value.array())
//
//  final def xmap[B](f: A => B, g: B => A): Converter[B] = new Converter[B] {
//    override def toByteArray(value: B): Attempt[Array[Byte]] = self.toByteArray(g(value))
//
//    override def fromByteArray(value: Array[Byte]): Attempt[B] = self.fromByteArray(value).map(f)
//  }
//}
//
//object Converter {
//  def apply[A](implicit inst: Converter[A]): Converter[A] = inst
//
//  implicit val byteConverter: Converter[Byte] = new Converter[Byte] {
//    override def toByteArray(value: Byte): Attempt[Array[Byte]] = Successful(Array(value))
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Byte] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Successful(value.head)
//      }
//  }
//
//  implicit val byteArrayConverter: Converter[Array[Byte]] = new Converter[Array[Byte]] {
//    override def toByteArray(value: Array[Byte]): Attempt[Array[Byte]] = Successful(value)
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Array[Byte]] = Successful(value)
//  }
//
//  implicit val booleanConverter: Converter[Boolean] = new Converter[Boolean] {
//    override def toByteArray(value: Boolean): Attempt[Array[Byte]] = {
//      val byte: Byte = if (value) 1 else 0
//      Successful(ByteBuffer.allocate(1).put(byte).array())
//    }
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Boolean] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          val byte = ByteBuffer.wrap(value).get()
//          if (byte == 1) {
//            true
//          } else {
//            false
//          }
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val shortConverter: Converter[Short] = new Converter[Short] {
//    override def toByteArray(value: Short): Attempt[Array[Byte]] =
//      Successful(ByteBuffer.allocate(2).putShort(value).array())
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Short] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          ByteBuffer.wrap(value).getShort
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val intConverter: Converter[Int] = new Converter[Int] {
//    override def toByteArray(value: Int): Attempt[Array[Byte]] =
//      Successful(ByteBuffer.allocate(4).putInt(value).array())
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Int] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          ByteBuffer.wrap(value).getInt()
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val longConverter: Converter[Long] = new Converter[Long] {
//    override def toByteArray(value: Long): Attempt[Array[Byte]] =
//      Successful(ByteBuffer.allocate(8).putLong(value).array())
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Long] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          ByteBuffer.wrap(value).getLong()
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val floatConverter: Converter[Float] = new Converter[Float] {
//    override def toByteArray(value: Float): Attempt[Array[Byte]] =
//      Successful(ByteBuffer.allocate(4).putFloat(value).array())
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Float] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          ByteBuffer.wrap(value).getFloat()
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val doubleConverter: Converter[Double] = new Converter[Double] {
//    override def toByteArray(value: Double): Attempt[Array[Byte]] =
//      Successful(ByteBuffer.allocate(8).putDouble(value).array())
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Double] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          ByteBuffer.wrap(value).getDouble()
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val stringConverter: Converter[String] = new Converter[String] {
//    override def toByteArray(value: String): Attempt[Array[Byte]] =
//      Successful(value.getBytes(StandardCharsets.UTF_8))
//
//    override def fromByteArray(value: Array[Byte]): Attempt[String] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          new String(value, StandardCharsets.UTF_8)
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//
//  implicit val charConverter: Converter[Char] = new Converter[Char] {
//    override def toByteArray(value: Char): Attempt[Array[Byte]] =
//      Successful(ByteBuffer.allocate(2).putChar(value).array())
//
//    override def fromByteArray(value: Array[Byte]): Attempt[Char] =
//      if (value.isEmpty) {
//        Failure(new IllegalArgumentException("Empty byte array"))
//      } else {
//        Try {
//          ByteBuffer.wrap(value).getChar()
//        } match {
//          case util.Failure(exception) => Failure(exception)
//          case Success(value)          => Successful(value)
//        }
//      }
//  }
//}
