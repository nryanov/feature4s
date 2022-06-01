lazy val kindProjectorVersion = "0.13.1"
// effect
lazy val zioVersion = "1.0.14"
lazy val catsVersion = "2.5.5"
// clients
lazy val curatorClientVersion = "5.2.1"
lazy val jedisVersion = "3.8.0"
lazy val lettuceVersion = "6.1.8.RELEASE"
lazy val redissonVersion = "3.17.2"
lazy val aerospikeClientVersion = "6.0.0"
// cache
lazy val caffeineVersion = "3.0.3"
// openapi
lazy val tapirVersion = "0.17.20"
lazy val akkaVersion = "2.6.19"
lazy val akkaHttpVersion = "10.2.9"
// test
lazy val scalatestVersion = "3.2.12"
lazy val scalamockVersion = "5.2.0"
lazy val testContainersVersion = "0.40.8"
lazy val logbackVersion = "1.2.11"

val scala2_12 = "2.12.13"
val scala2_13 = "2.13.6"

val compileAndTest = "compile->compile;test->test"

parallelExecution in Global := false

lazy val buildSettings = Seq(
  sonatypeProfileName := "com.nryanov",
  organization := "com.nryanov.feature4s",
  homepage := Some(url("https://github.com/nryanov/feature4s")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "nryanov",
      "Nikita Ryanov",
      "ryanov.nikita@gmail.com",
      url("https://nryanov.com")
    )
  ),
  scalaVersion := scala2_13,
  crossScalaVersions := Seq(scala2_12, scala2_13)
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

def compilerOptions(scalaVersion: String) = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint",
  "-language:existentials",
  "-language:postfixOps"
) ++ (CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, scalaMajor)) if scalaMajor == 12 => scala212CompilerOptions
  case Some((2, scalaMajor)) if scalaMajor == 13 => scala213CompilerOptions
})

lazy val scala212CompilerOptions = Seq(
  "-Yno-adapted-args",
  "-Ywarn-unused-import",
  "-Ypartial-unification",
  "-Xfuture"
)

lazy val scala213CompilerOptions = Seq(
  "-Wunused:imports"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions(scalaVersion.value),
  addCompilerPlugin(
    ("org.typelevel" %% "kind-projector" % kindProjectorVersion).cross(CrossVersion.full)
  ),
  Test / parallelExecution := false
)

lazy val allSettings = commonSettings ++ buildSettings

lazy val feature4s =
  project
    .in(file("."))
    .settings(name := "feature4s")
    .settings(allSettings)
    .settings(noPublish)
    .aggregate(
      core,
      cats,
      zio,
      redisCommon,
      jedis,
      jedisCats,
      jedisZio,
      lettuce,
      lettuceCats,
      lettuceZio,
      redisson,
      redissonCats,
      redissonZio,
      aerospike,
      aerospikeCats,
      aerospikeZio,
      zookeeper,
      tapir,
      tapirAkka,
      tapirHttp4s,
      tapirZioHttp4s,
      tapirCirce,
      tapirSprayJson,
      tapirJson4s,
      tapirTethys,
      examples
    )

lazy val core = project
  .in(file("modules/core"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-core")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
      "org.scalamock" %% "scalamock" % scalamockVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
      "com.dimafeng" %% "testcontainers-scala" % testContainersVersion % Test
    )
  )

lazy val cats = project
  .in(file("modules/effects/cats"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-cats")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsVersion
    )
  )
  .dependsOn(core % compileAndTest)

lazy val zio = project
  .in(file("modules/effects/zio"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-zio")
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion
    )
  )
  .dependsOn(core % compileAndTest)

lazy val redisCommon = project
  .in(file("modules/redis"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-redis")
  .dependsOn(core % compileAndTest)

lazy val jedis = project
  .in(file("modules/redis/jedis"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-jedis")
  .settings(
    libraryDependencies ++= Seq(
      "redis.clients" % "jedis" % jedisVersion
    )
  )
  .dependsOn(redisCommon % compileAndTest)

lazy val jedisCats = project
  .in(file("modules/redis/jedis/cats"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-jedis-cats")
  .dependsOn(jedis % compileAndTest)
  .dependsOn(cats % compileAndTest)

lazy val jedisZio = project
  .in(file("modules/redis/jedis/zio"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-jedis-zio")
  .dependsOn(jedis % compileAndTest)
  .dependsOn(zio % compileAndTest)

lazy val lettuce = project
  .in(file("modules/redis/lettuce"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-lettuce")
  .settings(
    libraryDependencies ++= Seq(
      "io.lettuce" % "lettuce-core" % lettuceVersion
    )
  )
  .dependsOn(redisCommon % compileAndTest)

lazy val lettuceCats = project
  .in(file("modules/redis/lettuce/cats"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-lettuce-cats")
  .dependsOn(lettuce % compileAndTest)
  .dependsOn(cats % compileAndTest)

lazy val lettuceZio = project
  .in(file("modules/redis/lettuce/zio"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-lettuce-zio")
  .dependsOn(lettuce % compileAndTest)
  .dependsOn(zio % compileAndTest)

lazy val redisson = project
  .in(file("modules/redis/redisson"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-redisson")
  .settings(
    libraryDependencies ++= Seq(
      "org.redisson" % "redisson" % redissonVersion
    )
  )
  .dependsOn(redisCommon % compileAndTest)

lazy val redissonCats = project
  .in(file("modules/redis/redisson/cats"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-redisson-cats")
  .dependsOn(redisson % compileAndTest)
  .dependsOn(cats % compileAndTest)

lazy val redissonZio = project
  .in(file("modules/redis/redisson/zio"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-redisson-zio")
  .dependsOn(redisson % compileAndTest)
  .dependsOn(zio % compileAndTest)

lazy val aerospike = project
  .in(file("modules/aerospike"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-aerospike")
  .settings(
    libraryDependencies ++= Seq(
      "com.aerospike" % "aerospike-client" % aerospikeClientVersion
    )
  )
  .dependsOn(core % compileAndTest)

lazy val aerospikeCats = project
  .in(file("modules/aerospike/cats"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-aerospike-cats")
  .dependsOn(aerospike % compileAndTest)
  .dependsOn(cats % compileAndTest)

lazy val aerospikeZio = project
  .in(file("modules/aerospike/zio"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-aerospike-zio")
  .dependsOn(aerospike % compileAndTest)
  .dependsOn(zio % compileAndTest)

lazy val zookeeper = project
  .in(file("modules/zookeeper"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-zookeeper")
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.curator" % "curator-framework" % curatorClientVersion,
      "org.apache.curator" % "curator-recipes" % curatorClientVersion,
      "org.apache.curator" % "curator-test" % curatorClientVersion % Test
    )
  )
  .dependsOn(core % compileAndTest)

lazy val zookeeperCats = project
  .in(file("modules/zookeeper/cats"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-zookeeper-cats")
  .dependsOn(zookeeper % compileAndTest)
  .dependsOn(cats % compileAndTest)

lazy val zookeeperZio = project
  .in(file("modules/zookeeper/zio"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-zookeeper-zio")
  .dependsOn(zookeeper % compileAndTest)
  .dependsOn(zio % compileAndTest)

lazy val cache = project
  .in(file("modules/cache"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-cache")
  .settings(
    libraryDependencies ++= Seq(
      "com.github.ben-manes.caffeine" % "caffeine" % caffeineVersion
    )
  )
  .dependsOn(core % compileAndTest)

lazy val tapir = project
  .in(file("modules/tapir"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server-tests" % tapirVersion % Test
    )
  )
  .dependsOn(core % compileAndTest)

lazy val tapirHttp4s = project
  .in(file("modules/tapir/http4s"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-http4s",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirAkka = project
  .in(file("modules/tapir/akka"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-akka",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirZioHttp4s = project
  .in(file("modules/tapir/zio-http4s"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-zio-http4s",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirCirce = project
  .in(file("modules/tapir/json/circe"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-json-circe",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirSprayJson = project
  .in(file("modules/tapir/json/sprayjson"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-json-sprayjson",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-json-spray" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirJson4s = project
  .in(file("modules/tapir/json/json4s"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-json-json4s",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-json-json4s" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirTethys = project
  .in(file("modules/tapir/json/tethys"))
  .settings(allSettings)
  .settings(
    name := "feature4s-tapir-json-tethys",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-json-tethys" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val examples = project
  .in(file("examples"))
  .settings(allSettings)
  .settings(noPublish)
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion
    )
  )
  .dependsOn(tapirHttp4s % compileAndTest)
  .dependsOn(tapirAkka % compileAndTest)
  .dependsOn(tapirZioHttp4s % compileAndTest)
  .dependsOn(tapirCirce % compileAndTest)
  .dependsOn(lettuceCats % compileAndTest)
  .dependsOn(lettuceZio % compileAndTest)
