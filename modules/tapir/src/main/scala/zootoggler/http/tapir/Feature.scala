//package zootoggler.http.tapir
//
//import zootoggler.core.FeatureView
//
//final case class Feature(
//  featureName: String,
//  typeName: String,
//  description: Option[String],
//  formattedValue: String
//)
//
//object Feature {
//  def apply(featureView: FeatureView): Feature = new Feature(
//    featureView.featureName,
//    featureView.typeName,
//    featureView.description,
//    featureView.formattedValue.require
//  )
//}
