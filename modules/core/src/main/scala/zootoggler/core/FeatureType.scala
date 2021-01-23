package zootoggler.core

trait FeatureType[T] {
  def typeName: String

  def converter: Converter[T]

  def prettyPrint(value: Array[Byte]): Attempt[String]

  def fromString(value: String): Attempt[T]

  final def fromStringToRaw(value: String): Attempt[Array[Byte]] =
    fromString(value).flatMap(converter.toByteArray)
}

object FeatureType {
  implicit val booleanFeatureType: FeatureType[Boolean] = new FeatureType[Boolean] {
    override val typeName: String = "boolean"
    override val converter: Converter[Boolean] = Converter.booleanConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Boolean] = Attempt.delay(value.toBoolean)
  }

  implicit val byteFeatureType: FeatureType[Byte] = new FeatureType[Byte] {
    override val typeName: String = "byte"
    override val converter: Converter[Byte] = Converter.byteConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Byte] = Attempt.delay(value.toByte)
  }

  implicit val shortFeatureType: FeatureType[Short] = new FeatureType[Short] {
    override val typeName: String = "short"
    override val converter: Converter[Short] = Converter.shortConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Short] = Attempt.delay(value.toShort)
  }

  implicit val intFeatureType: FeatureType[Int] = new FeatureType[Int] {
    override val typeName: String = "int"
    override val converter: Converter[Int] = Converter.intConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Int] = Attempt.delay(value.toInt)
  }

  implicit val longFeatureType: FeatureType[Long] = new FeatureType[Long] {
    override val typeName: String = "long"
    override val converter: Converter[Long] = Converter.longConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Long] = Attempt.delay(value.toLong)
  }

  implicit val floatFeatureType: FeatureType[Float] = new FeatureType[Float] {
    override val typeName: String = "float"
    override val converter: Converter[Float] = Converter.floatConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Float] = Attempt.delay(value.toFloat)
  }

  implicit val doubleFeatureType: FeatureType[Double] = new FeatureType[Double] {
    override val typeName: String = "double"
    override val converter: Converter[Double] = Converter.doubleConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Double] = Attempt.delay(value.toDouble)
  }

  implicit val stringFeatureType: FeatureType[String] = new FeatureType[String] {
    override val typeName: String = "string"
    override val converter: Converter[String] = Converter.stringConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] = converter.fromByteArray(value)
    override def fromString(value: String): Attempt[String] = Attempt.successful(value)
  }

  implicit val charFeatureType: FeatureType[Char] = new FeatureType[Char] {
    override val typeName: String = "char"
    override val converter: Converter[Char] = Converter.charConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.toString)
    override def fromString(value: String): Attempt[Char] = Attempt.delay(value.head)
  }

  implicit val byteArrayFeatureType: FeatureType[Array[Byte]] = new FeatureType[Array[Byte]] {
    override val typeName: String = "Array[Byte]"
    override val converter: Converter[Array[Byte]] = Converter.byteArrayConverter
    override def prettyPrint(value: Array[Byte]): Attempt[String] =
      converter.fromByteArray(value).map(_.mkString("[", ", ", "]"))
    override def fromString(value: String): Attempt[Array[Byte]] =
      Attempt.successful(value.getBytes)
  }

}
