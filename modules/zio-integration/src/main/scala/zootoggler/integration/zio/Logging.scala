package zootoggler.integration.zio

import org.slf4j.LoggerFactory
import zio.UIO

private[zio] trait Logging {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def debug(msg: String): UIO[Unit] = UIO.effectTotal(logger.debug(msg))

  def info(msg: String): UIO[Unit] = UIO.effectTotal(logger.info(msg))

  def warn(msg: String): UIO[Unit] = UIO.effectTotal(logger.warn(msg))

  def error(msg: String): UIO[Unit] = UIO.effectTotal(logger.error(msg))

  def error(msg: String, cause: Throwable): UIO[Unit] = UIO.effectTotal(logger.error(msg))
}
