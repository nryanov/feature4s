package zootoggler.core.configuration

import org.apache.curator.RetryPolicy
import org.apache.curator.retry.{
  ExponentialBackoffRetry,
  RetryForever,
  RetryNTimes,
  RetryOneTime,
  RetryUntilElapsed
}
import zootoggler.core.configuration.RetryPolicyType._

final case class ZtConfiguration(
  connectionString: String,
  retryPolicyType: RetryPolicyType
) {
  val retryPolicy: RetryPolicy = retryPolicyType match {
    case Exponential(sleepMsBetweenRetries, retries) =>
      new ExponentialBackoffRetry(sleepMsBetweenRetries, retries)
    case Forever(retryIntervalMs) => new RetryForever(retryIntervalMs)
    case NTimes(sleepMsBetweenRetries, retries) =>
      new RetryNTimes(retries, sleepMsBetweenRetries)
    case OneTime(sleepMsBetweenRetries) => new RetryOneTime(sleepMsBetweenRetries)
    case UntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries) =>
      new RetryUntilElapsed(maxElapsedTimeMs, sleepMsBetweenRetries)
  }
}
