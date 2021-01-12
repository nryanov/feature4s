package zootoggler.integration.cats

import org.slf4j.LoggerFactory
import cats.effect.Sync

private[cats] trait Logging[F[_]] {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def debug(msg: String)(implicit F: Sync[F]): F[Unit] = F.delay(logger.debug(msg))

  def info(msg: String)(implicit F: Sync[F]): F[Unit] = F.delay(logger.info(msg))

  def warn(msg: String)(implicit F: Sync[F]): F[Unit] = F.delay(logger.warn(msg))

  def error(msg: String)(implicit F: Sync[F]): F[Unit] = F.delay(logger.error(msg))

  def error(msg: String, cause: Throwable)(implicit F: Sync[F]): F[Unit] =
    F.delay(logger.error(msg, cause))
}
