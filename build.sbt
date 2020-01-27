name := "sttp-akka-monix"
organization := "com.fullfacing"

version      := "1.0.3"
scalaVersion := "2.13.1"
organization := "com.fullfacing"

crossScalaVersions := Seq(scalaVersion.value, "2.12.10")

val baseScalaOpts = Seq(
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

val scalac213Opts = baseScalaOpts
val scalac212Opts = baseScalaOpts ++ Seq("-Ypartial-unification")

scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, n)) if n <= 12 => scalac212Opts
  case _                       => scalac213Opts
})

val akka: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.6.2",
  "com.typesafe.akka" %% "akka-http" % "10.1.11"
)

val sttp: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp" %% "core" % "1.7.2",
)

val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % "3.1.0"
)

libraryDependencies ++= akka ++ sttp ++ monix

publishArtifact := true
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)