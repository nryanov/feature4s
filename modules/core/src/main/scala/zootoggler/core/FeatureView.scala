//package zootoggler.core
//
//final case class FeatureView(
//  featureName: String,
//  typeName: String,
//  description: Option[String],
//  formattedValue: Attempt[String]
//)
//
//object FeatureView {
//  def apply(
//    featureName: String,
//    typeName: String,
//    description: Option[String],
//    formattedValue: Attempt[String]
//  ): FeatureView = new FeatureView(featureName, typeName, description, formattedValue)
//
//  def apply(
//    featureName: String,
//    typeName: String,
//    formattedValue: Attempt[String]
//  ): FeatureView = new FeatureView(featureName, typeName, None, formattedValue)
//
//  def apply[A](
//    featureInfo: FeatureInfo[A],
//    currentValue: Array[Byte]
//  ): FeatureView =
//    FeatureView(
//      featureInfo.name,
//      featureInfo.featureType.typeName,
//      featureInfo.description,
//      featureInfo.featureType.prettyPrint(currentValue)
//    )
//}
