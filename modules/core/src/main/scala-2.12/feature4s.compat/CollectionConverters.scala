package feature4s.compat

import scala.collection.JavaConverters._

object CollectionConverters {
  implicit def scalaMapToJava[A, B](map: Map[A, B]): java.util.Map[A, B] = map.asJava

  implicit def javaMapToScala[A, B](map: java.util.Map[A, B]): Map[A, B] = map.asScala.toMap

  implicit def javaListToScala[A](list: java.util.List[A]): List[A] = list.asScala.toList

  implicit def javaIterableToScala[A](iter: java.lang.Iterable[A]): Iterable[A] = iter.asScala

  implicit def javaIteratorToScala[A](iter: java.util.Iterator[A]): Iterator[A] = iter.asScala

  implicit def scalaSetTOJava[A](set: Set[A]): java.util.Set[A] = set.asJava
}
