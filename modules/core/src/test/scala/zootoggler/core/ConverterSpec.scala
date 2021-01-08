package zootoggler.core

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import zootoggler.core.Converter._
import zootoggler.Generators._
import zootoggler.BaseSpec

class ConverterSpec extends BaseSpec with OptionValues with ScalaCheckPropertyChecks {
  "Converter" should {
    "convert byte" in {
      forAll(byte()) { value =>
        val converter = Converter[Byte]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe Array(value).toList
        fromArray shouldBe value
      }
    }

    "convert byte array" in {
      forAll(nonEmptyListOf(128, byte())) { value =>
        val converter = Converter[Array[Byte]]
        val toArray = converter.toByteArray(value.toArray).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe value
        fromArray.toList shouldBe value
      }
    }

    "convert boolean" in {
      forAll(bool()) { value =>
        val converter = Converter[Boolean]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        if (value) {
          toArray.toList shouldBe Array[Byte](1).toList
        } else {
          toArray.toList shouldBe Array[Byte](0).toList
        }
        fromArray shouldBe value
      }
    }

    "convert char" in {
      forAll(char()) { value =>
        val converter = Converter[Char]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe ByteBuffer.allocate(2).putChar(value).array().toList
        fromArray shouldBe value
      }
    }

    "convert short" in {
      forAll(short()) { value =>
        val converter = Converter[Short]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe ByteBuffer.allocate(2).putShort(value).array().toList
        fromArray shouldBe value
      }
    }

    "convert int" in {
      forAll(int()) { value =>
        val converter = Converter[Int]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe ByteBuffer.allocate(4).putInt(value).array().toList
        fromArray shouldBe value
      }
    }

    "convert long" in {
      forAll(long()) { value =>
        val converter = Converter[Long]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe ByteBuffer.allocate(8).putLong(value).array().toList
        fromArray shouldBe value
      }
    }

    "convert float" in {
      forAll(float()) { value =>
        val converter = Converter[Float]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe ByteBuffer.allocate(4).putFloat(value).array().toList
        fromArray shouldBe value
      }
    }

    "convert double" in {
      forAll(double()) { value =>
        val converter = Converter[Double]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe ByteBuffer.allocate(8).putDouble(value).array().toList
        fromArray shouldBe value
      }
    }

    "convert string" in {
      forAll(nonEmptyString(128)) { value =>
        val converter = Converter[String]
        val toArray = converter.toByteArray(value).toOption.value
        val fromArray = converter.fromByteArray(toArray).toOption.value

        toArray.toList shouldBe value.getBytes(StandardCharsets.UTF_8).toList
        fromArray shouldBe value
      }
    }
  }
}
