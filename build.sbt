lazy val kindProjectorVersion = "0.13.0"
lazy val enumeratumVersion = "1.6.1"
// effect
lazy val zioVersion = "1.0.3"
lazy val catsVersion = "2.3.0"
// clients
lazy val curatorClientVersion = "5.1.0"
// openapi
lazy val tapirVersion = "0.17.5"
// logging
lazy val slf4jApiVersion = "1.7.30"
// test
lazy val scalatestVersion = "3.2.0"
lazy val scalacheckPlusVersion = "3.2.0.0"
lazy val scalamockVersion = "5.0.0"
lazy val scalacheckVersion = "1.14.3"
lazy val testContainersVersion = "0.38.7"
lazy val logbackVersion = "1.2.3"

val scala2_12 = "2.12.13"
val scala2_13 = "2.13.5"

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
    .aggregate(core)

lazy val core = project
  .in(file("modules/core"))
  .settings(allSettings)
  .settings(moduleName := "feature4s-core")
  .settings(
    libraryDependencies ++= Seq(
      "com.beachape" %% "enumeratum" % enumeratumVersion,
      "org.slf4j" % "slf4j-api" % slf4jApiVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % scalacheckPlusVersion % Test,
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

lazy val tapir = project
  .in(file("modules/tapir"))
  .settings(allSettings)
  .settings(
    name := "tapir",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion
    )
  )
  .dependsOn(core % compileAndTest)

lazy val tapirHttp4s = project
  .in(file("modules/tapir-http4s"))
  .settings(allSettings)
  .settings(
    name := "tapir-http4s",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirAkka = project
  .in(file("modules/tapir-akka"))
  .settings(allSettings)
  .settings(
    name := "tapir-akka",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)

lazy val tapirZioHttp4s = project
  .in(file("modules/tapir-zio-http4s"))
  .settings(allSettings)
  .settings(
    name := "tapir-zio-http4s",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % tapirVersion
    )
  )
  .dependsOn(tapir % compileAndTest)
