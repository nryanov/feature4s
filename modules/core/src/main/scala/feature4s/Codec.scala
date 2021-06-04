package feature4s

trait Codec[A] {
  def encode(value: A): Either[EncodingError, String]

  def encodeFromString(value: String): Either[EncodingError, String]

  def decode(value: String): Either[DecodingError, A]
}
