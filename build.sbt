import sbt.Keys.{publishLocalConfiguration, scalaVersion}

val scalaV = "2.13.1"
val scalacOpts = Seq(
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

// Global sbt project settings.
lazy val global = {
  Seq(
    version := "1.2.0",
    organization := "com.fullfacing",
    scalaVersion := scalaV,
    crossScalaVersions := Seq(scalaVersion.value, "2.12.10"),
    scalacOptions ++= scalacOpts ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 => scalacOpts ++ Seq("-Ypartial-unification")
      case _ => scalacOpts
    }),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
    ),
    libraryDependencies ++= akka ++ monix ++ sttp,
    publishTo := sonatypePublishToBundle.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  )
}

val akka: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.6.4",
  "com.typesafe.akka" %% "akka-http" % "10.1.11"
)

val sttp: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp.client" %% "core" % "2.0.7"
)

val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % "3.1.0"
)

val `monix-bio`: Seq[ModuleID] = Seq(
  "io.monix" %% "monix-bio" % "0.1.0"
)

lazy val `sttp-akka-monix-core` = (project in file("./sttp-akka-monix-core"))
  .settings(global: _*)
  .settings(name := "sttp-akka-monix-core", publishArtifact := true)


lazy val `sttp-akka-monix-task` = (project in file("./sttp-akka-monix-task"))
  .settings(global: _*)
  .settings(name := "sttp-akka-monix-task", publishArtifact := true)
  .dependsOn(`sttp-akka-monix-core`)

lazy val `sttp-akka-monix-bio` = (project in file("./sttp-akka-monix-bio"))
  .settings(global: _*)
  .settings(libraryDependencies ++= `monix-bio`)
  .settings(name := "sttp-akka-monix-bio", publishArtifact := true)
  .dependsOn(`sttp-akka-monix-core`)

lazy val root = (project in file("."))
  .settings(global: _*)
  .settings(publishArtifact := false)
  .aggregate(
    `sttp-akka-monix-task`,
    `sttp-akka-monix-bio`
  )