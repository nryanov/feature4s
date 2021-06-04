//package zootoggler.core
//
//final case class FeatureInfo[A](
//  name: String,
//  featureType: FeatureType[A],
//  description: Option[String]
//)
//
//object FeatureInfo {
//  def apply[A](
//    name: String,
//    featureType: FeatureType[A],
//    description: Option[String]
//  ): FeatureInfo[A] = new FeatureInfo(name, featureType, description)
//
//  def apply[A](
//    name: String,
//    featureType: FeatureType[A]
//  ): FeatureInfo[A] = new FeatureInfo(name, featureType, None)
//}
