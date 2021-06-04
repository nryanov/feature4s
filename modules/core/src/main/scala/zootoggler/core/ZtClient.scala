//package zootoggler.core
//
//trait ZtClient[F[_]] {
//  def register[A: FeatureType](
//    defaultValue: A,
//    featureName: String,
//    description: Option[String]
//  ): F[FeatureAccessor[F, A]]
//
//  final def register[A: FeatureType](
//    defaultValue: A,
//    featureName: String
//  ): F[FeatureAccessor[F, A]] = register(defaultValue, featureName, None)
//
//  def update[A: FeatureType](
//    featureName: String,
//    newValue: A,
//    description: Option[String]
//  ): F[Boolean]
//
//  def updateFromString(
//    featureName: String,
//    newValue: String,
//    description: Option[String]
//  ): F[Boolean]
//
//  final def updateFromString(
//    featureName: String,
//    newValue: String
//  ): F[Boolean] = updateFromString(featureName, newValue, None)
//
//  def updateFromByteArray(
//    featureName: String,
//    newValue: Array[Byte],
//    description: Option[String]
//  ): F[Boolean]
//
//  final def updateFromByteArray(
//    featureName: String,
//    newValue: Array[Byte]
//  ): F[Boolean] = updateFromByteArray(featureName, newValue, None)
//
//  final def update[A: FeatureType](
//    featureName: String,
//    newValue: A
//  ): F[Boolean] = update(featureName, newValue, None)
//
//  def remove(featureName: String): F[Boolean]
//
//  def isExist(featureName: String): F[Boolean]
//
//  def recreate[A: FeatureType](
//    defaultValue: A,
//    featureName: String,
//    description: Option[String]
//  ): F[FeatureAccessor[F, A]]
//
//  final def recreate[A: FeatureType](
//    defaultValue: A,
//    featureName: String
//  ): F[FeatureAccessor[F, A]] = recreate(defaultValue, featureName, None)
//
//  def featureList(): List[FeatureView]
//
//  def close(): F[Unit]
//}
