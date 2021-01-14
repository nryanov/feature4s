package zootoggler.core.configuration

final case class FeatureConfiguration(rootPath: String, namespace: Option[String])

object FeatureConfiguration {
  def apply(rootPath: String, namespace: Option[String]): FeatureConfiguration =
    new FeatureConfiguration(rootPath, namespace)

  def apply(rootPath: String): FeatureConfiguration =
    new FeatureConfiguration(rootPath, None)
}
