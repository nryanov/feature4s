//package zootoggler.core.configuration
//
//sealed trait RetryPolicyType
//
//object RetryPolicyType {
//  final case class Exponential(sleepMsBetweenRetries: Int, retries: Int) extends RetryPolicyType
//
//  final case class Forever(retryIntervalMs: Int) extends RetryPolicyType
//
//  final case class NTimes(sleepMsBetweenRetries: Int, retries: Int) extends RetryPolicyType
//
//  final case class OneTime(sleepMsBetweenRetries: Int) extends RetryPolicyType
//
//  final case class UntilElapsed(maxElapsedTimeMs: Int, sleepMsBetweenRetries: Int)
//      extends RetryPolicyType
//}
