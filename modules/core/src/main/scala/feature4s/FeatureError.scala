package feature4s

import scala.util.control.NoStackTrace

sealed trait FeatureError extends Exception with NoStackTrace

final case class InternalError(cause: Throwable)
    extends Exception(cause.getLocalizedMessage, cause)
    with FeatureError

final case class EncodingError(msg: String) extends Exception(msg) with FeatureError

final case class DecodingError(msg: String) extends Exception(msg) with FeatureError

final case class FeatureNotFound(name: String)
    extends Exception(s"Feature `$name` not found")
    with FeatureError
