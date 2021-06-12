package feature4s.zookeeper

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

private[zookeeper] object StateSerDe {
  def stateToBytes(isEnable: Boolean, description: Option[String]): Array[Byte] = {
    val flag: Byte = if (isEnable) 0x1.toByte else 0x0.toByte
    val (size: Int, data: Array[Byte]) = description match {
      case Some(value) =>
        val bytes = value.getBytes(StandardCharsets.UTF_8)
        (bytes.length, bytes)
      case None => (0, Array.empty[Byte])
    }

    val buffer = ByteBuffer.allocate(1 + 4 + size)
    buffer.put(flag)
    buffer.putInt(size)
    buffer.put(data)

    buffer.array()
  }

  def stateFromBytes(state: Array[Byte]): (Boolean, Option[String]) = {
    val buffer = ByteBuffer.wrap(state)
    val isEnable = if (buffer.get() == 0x0.toByte) false else true
    val descriptionSize = buffer.getInt

    val description =
      if (descriptionSize == 0) None
      else {
        val data = new Array[Byte](descriptionSize)
        buffer.get(data)
        Some(new String(data, StandardCharsets.UTF_8))
      }

    (isEnable, description)
  }
}
