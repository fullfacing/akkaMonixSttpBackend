name := "sttp-akka-monix"
organization := "com.fullfacing"

version  := "1.0.0"
scalaVersion := "2.12.8"
organization := "com.fullfacing"

scalacOptions ++= Seq(
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:params",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates",
  "-Ypartial-unification",
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

val akka: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.23",
  "com.typesafe.akka" %% "akka-http" % "10.1.8"
)

val cats: Seq[ModuleID] = Seq(
  "org.typelevel" %% "cats-core" % "1.6.0"
)

val sttp: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp" %% "core" % "1.5.17",
)

val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % "3.0.0-RC2"
)

libraryDependencies ++= akka ++ sttp ++ cats ++ monix

publishArtifact := true
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)