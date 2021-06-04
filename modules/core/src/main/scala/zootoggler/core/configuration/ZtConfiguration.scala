//package zootoggler.core.configuration
//
//import org.apache.curator.RetryPolicy
//import org.apache.curator.retry.{
//  ExponentialBackoffRetry,
//  RetryForever,
//  RetryNTimes,
//  RetryOneTime,
//  RetryUntilElapsed
//}
//import zootoggler.core.configuration.RetryPolicyType._
//
//final case class ZtConfiguration(
//  connectionString: String,
//  rootPath: String,
//  namespace: Option[String],
//  retryPolicyType: RetryPolicyType,
//  connectionTimeoutMs: Int,
//  cacheBuildTimeoutMs: Int,
//  featureRegisterTimeoutMs: Int
//) {
//  val retryPolicy: RetryPolicy = retryPolicyType match {
//    case Exponential(sleepMsBetweenRetries, retries) =>
//      new ExponentialBackoffRetry(sleepMsBetweenRetries, retries)
//    case Forever(retryIntervalMs) => new RetryForever(retryIntervalMs)
//    case NTimes(sleepMsBetweenRetries, retries) =>
//      new RetryNTimes(retries, sleepMsBetweenRetries)
//    case OneTime(sleepMsBetweenRetries) => new RetryOneTime(sleepMsBetweenRetries)
//    case UntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries) =>
//      new RetryUntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries)
//  }
//}
//
//object ZtConfiguration {
//  def apply(
//    connectionString: String,
//    rootPath: String,
//    namespace: Option[String],
//    retryPolicyType: RetryPolicyType,
//    connectionTimeoutMs: Int,
//    cacheBuildTimeoutMs: Int,
//    featureRegisterTimeoutMs: Int
//  ): ZtConfiguration = new ZtConfiguration(
//    connectionString,
//    rootPath,
//    namespace,
//    retryPolicyType,
//    connectionTimeoutMs,
//    cacheBuildTimeoutMs,
//    featureRegisterTimeoutMs
//  )
//
//  def apply(
//    connectionString: String,
//    rootPath: String,
//    namespace: Option[String],
//    retryPolicyType: RetryPolicyType
//  ): ZtConfiguration =
//    new ZtConfiguration(connectionString, rootPath, namespace, retryPolicyType, 5000, 5000, 5000)
//
//  def apply(
//    connectionString: String,
//    rootPath: String,
//    retryPolicyType: RetryPolicyType
//  ): ZtConfiguration =
//    new ZtConfiguration(connectionString, rootPath, None, retryPolicyType, 5000, 5000, 5000)
//}
