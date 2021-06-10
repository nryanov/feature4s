package feature4s

import com.aerospike.client.Key

package object aerospike {
  val DefaultSetName: String = "features"

  val FeatureNameFieldName: String = "name"
  val ValueFieldName: String = "value"
  val DescriptionFieldName: String = "description"

  def key(featureName: String, namespace: String): Key =
    new Key(namespace, DefaultSetName, featureName)
}
