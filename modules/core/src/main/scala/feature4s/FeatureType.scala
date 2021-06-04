package feature4s

import java.nio.ByteBuffer

import enumeratum._

import scala.util.Try

sealed trait FeatureType[A] extends EnumEntry {
  def codec: Codec[A]
}

object FeatureType extends Enum[FeatureType[_]] {

  override def values: IndexedSeq[FeatureType[_]] = findValues

  implicit case object BooleanType extends FeatureType[Boolean] {
    override def codec: Codec[Boolean] = ???
  }

  implicit case object StringType extends FeatureType[String] {
    override def codec: Codec[String] = new Codec[String] {
      override def encode(value: String): Either[EncodingError, String] = Right(value)

      override def encodeFromString(value: String): Either[EncodingError, String] = encode(value)

      override def decode(value: String): Either[DecodingError, String] = Right(value)
    }
  }

  implicit case object ShortType extends FeatureType[Short] {
    override def codec: Codec[Short] = ???
  }

  implicit case object IntegerType extends FeatureType[Int] {
    override def codec: Codec[Int] = new Codec[Int] {
      override def encode(value: Int): Either[EncodingError, String] = Right(value.toString)

      override def encodeFromString(value: String): Either[EncodingError, String] =
        Try(value.toInt).toEither.left
          .map(err => EncodingError(err.getLocalizedMessage))
          .map(_ => value)

      override def decode(value: String): Either[DecodingError, Int] =
        Try(value.toInt).toEither.left.map(err => DecodingError(err.getLocalizedMessage))
    }
  }

  implicit case object LongType extends FeatureType[Long] {
    override def codec: Codec[Long] = ???
  }

  implicit case object FloatType extends FeatureType[Float] {
    override def codec: Codec[Float] = ???
  }

  implicit case object DoubleType extends FeatureType[Double] {
    override def codec: Codec[Double] = ???
  }
}
