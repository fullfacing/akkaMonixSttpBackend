name := "sttp-akka-monix"
organization := "com.fullfacing"

version  := "1.0.2"
scalaVersion := "2.13.0"
organization := "com.fullfacing"

scalacOptions ++= Seq(
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

val akka: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.25",
  "com.typesafe.akka" %% "akka-http" % "10.1.9"
)

val cats: Seq[ModuleID] = Seq(
  "org.typelevel" %% "cats-core" % "2.0.0"
)

val sttp: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp" %% "core" % "1.6.6",
)

val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % "3.0.0"
)

libraryDependencies ++= akka ++ sttp ++ cats ++ monix

publishArtifact := true
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)