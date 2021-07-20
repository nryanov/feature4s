package feature4s.cache

import feature4s.monad.IdMonadError
import feature4s.{Feature, FeatureRegistry, FeatureState, Id}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class CachedFeatureRegistryAdditionalSpec extends AnyFunSuite with Matchers with MockFactory {
  test("register") {
    var counter = 0
    val featureRegistry = mock[FeatureRegistry[Id]]
    val feature: Feature[Id] = Feature[Id](
      "feature",
      () => {
        counter += 1
        true
      },
      None
    )

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.register _).expects("feature", true, None).returning(feature).once()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    val cachedFeature = cache.register("feature", enable = true, None)

    // cached call
    cachedFeature.isEnable() shouldBe true
    // counter increased by 1 because of getting current actual value
    counter shouldBe 1
  }

  test("register (zero ttl)") {
    var counter = 0
    val featureRegistry = mock[FeatureRegistry[Id]]
    val feature: Feature[Id] = Feature[Id](
      "feature",
      () => {
        counter += 1
        true
      },
      None
    )

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.register _).expects("feature", true, None).returning(feature).once()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 0.seconds)
    val cachedFeature = cache.register("feature", enable = true, None)

    // cached call (value was not cached due to zero ttl)
    cachedFeature.isEnable() shouldBe true
    counter shouldBe 2
  }

  test("recreate") {
    var counter = 0
    val featureRegistry = mock[FeatureRegistry[Id]]
    val feature: Feature[Id] = Feature[Id](
      "feature",
      () => {
        counter += 1
        true
      },
      None
    )

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.recreate _).expects("feature", true, None).returning(feature).once()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    val cachedFeature = cache.recreate("feature", enable = true, None)

    // cached call
    cachedFeature.isEnable() shouldBe true
    counter shouldBe 0
  }

  test("remove") {
    val featureRegistry = mock[FeatureRegistry[Id]]
    val feature: Feature[Id] = Feature[Id](
      "feature",
      () => true,
      None
    )

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.recreate _).expects("feature", true, None).returning(feature).once()
    (featureRegistry.remove _).expects("feature").returning(true).once()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    cache.recreate("feature", enable = true, None)
    cache.remove("feature") shouldBe true
  }

  test("update") {
    val featureRegistry = mock[FeatureRegistry[Id]]
    val feature: Feature[Id] = Feature[Id](
      "feature",
      () => true,
      None
    )

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.recreate _).expects("feature", true, None).returning(feature).once()
    (featureRegistry.update _).expects("feature", false).returning().once()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    cache.recreate("feature", enable = true, None)
    cache.update("feature", enable = false) shouldBe (())
  }

  test("isExist -- cached call") {
    val featureRegistry = mock[FeatureRegistry[Id]]
    val feature: Feature[Id] = Feature[Id](
      "feature",
      () => true,
      None
    )

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.recreate _).expects("feature", true, None).returning(feature).once()
    (featureRegistry.isExist _).expects("feature").returning(true).never()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    cache.recreate("feature", enable = true, None)
    // cached call
    cache.isExist("feature") shouldBe true
  }

  test("isExist -- not cached call") {
    val featureRegistry = mock[FeatureRegistry[Id]]

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _).expects().returning(List.empty).once()
    (featureRegistry.isExist _).expects("feature").returning(false).once()

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    // not cached call
    cache.isExist("feature") shouldBe false
  }

  test("directly access featureList") {
    val featureRegistry = mock[FeatureRegistry[Id]]

    (featureRegistry.monadError _).expects().returning(IdMonadError).once()
    (featureRegistry.featureList _)
      .expects()
      .returning(List(FeatureState("test", isEnable = true, None)))
      .twice() // on creation and on call

    val cache = CachedFeatureRegistry.wrap(featureRegistry, 5.minutes)
    cache.featureList() shouldBe List(FeatureState("test", isEnable = true, None))
  }
}
