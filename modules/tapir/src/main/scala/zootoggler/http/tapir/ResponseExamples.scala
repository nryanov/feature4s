package zootoggler.http.tapir

import zootoggler.http.tapir.FeatureUpdate._

private[http] object ResponseExamples {
  val stringInputExample: StringInput = StringInput(
    "featureName",
    Some("description"),
    "newValue"
  )

  val byteArrayInputExample: ByteArrayInput = ByteArrayInput(
    "featureName",
    Some("description"),
    Array(1, 2, 3, 4, 5)
  )

  val featureListExample: List[Feature] = List(
    Feature("featureName", "typeName", Some("description"), "value")
  )
}
