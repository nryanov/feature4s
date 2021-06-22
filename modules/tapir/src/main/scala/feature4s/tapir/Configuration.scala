package feature4s.tapir

/**
 * @param allowRemove - if true then API will contain a route which allows to delete feature
 *                    otherwise API will not contain it.
 */
final case class Configuration(allowRemove: Boolean)

object Configuration {
  val Default: Configuration = Configuration(allowRemove = false)

  def apply(allowRemove: Boolean): Configuration = new Configuration(allowRemove)
}
