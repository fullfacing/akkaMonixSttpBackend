import sbt.Keys.{publishLocalConfiguration, scalaVersion}
import ReleaseTransformations._

import sbt.url
import xerial.sbt.Sonatype.GitHubHosting

val scalaV = "2.13.11"
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
    organization := "com.fullfacing",
    scalaVersion := scalaV,
    crossScalaVersions := Seq(scalaVersion.value, "2.12.14"),
    scalacOptions ++= scalacOpts ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 =>
        scalacOpts ++ Seq(
          "-Ypartial-unification",
          "-Xfuture",
          "-Yno-adapted-args",
          "-Ywarn-unused-import"
        )
      case _ => scalacOpts
    }),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases")),

    credentials += Credentials("GnuPG Key ID", "gpg", "419C90FB607D11B0A7FE51CFDAF842ABC601C14F", "ignored"),

    publishTo := sonatypePublishToBundle.value,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),

    // Your profile name of the sonatype account. The default is the same with the organization value
    sonatypeProfileName := "com.fullfacing",

    // To sync with Maven central, you need to supply the following information:
    publishMavenStyle := true,

    // MIT Licence
    licenses  := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),

    // Github Project Information
    sonatypeProjectHosting := Some(GitHubHosting("fullfacing", "sttp-akka-monix", "curious@fullfacing.com")),

    // Developer Contact Information
    developers := List(
      Developer(
        id    = "lmuller90",
        name  = "Louis Muller",
        email = "lmuller@fullfacing.com",
        url   = url("https://www.fullfacing.com/")
      ),
      Developer(
        id    = "neil-fladderak",
        name  = "Neil Fladderak",
        email = "neil@fullfacing.com",
        url   = url("https://www.fullfacing.com/")
      ),
      Developer(
        id    = "execution1939",
        name  = "Richard Peters",
        email = "rpeters@fullfacing.com",
        url   = url("https://www.fullfacing.com/")
      )
    ),

    releaseIgnoreUntrackedFiles := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseCrossBuild := true,
    releaseVersionBump := sbtrelease.Version.Bump.Minor,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      tagRelease,
      pushChanges,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      swapToDevelopAction,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}

import sbtrelease.Git
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.Utilities._

def swapToDevelop: State => State = { st: State =>
  val git = st.extract.get(releaseVcs).get.asInstanceOf[Git]
  git.cmd("checkout", "develop") ! st.log
  st
}

lazy val swapToDevelopAction = { st: State =>
  val newState = swapToDevelop(st)
  newState
}

val akka: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.6.15",
  "com.typesafe.akka" %% "akka-http"   % "10.2.4"
)

val sttp: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp.client" %% "core" % "2.2.9"
)

val sttp3: Seq[ModuleID] = Seq(
  "com.softwaremill.sttp.client3" %% "core" % "3.3.6",
)

val monix: Seq[ModuleID] = Seq(
  "io.monix" %% "monix" % "3.4.0"
)

val `monix-bio`: Seq[ModuleID] = Seq(
  "io.monix" %% "monix-bio" % "1.2.0"
)

lazy val `sttp-akka-monix-core` = (project in file("./sttp-akka-monix-core"))
  .settings(global: _*)
  .settings(libraryDependencies ++= akka ++ monix ++ sttp)
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

lazy val `sttp3-akka-monix-core` = (project in file("./sttp3-akka-monix-core"))
  .settings(global: _*)
  .settings(libraryDependencies ++= akka ++ monix ++ sttp3)
  .settings(name := "sttp3-akka-monix-core", publishArtifact := true)

lazy val `sttp3-akka-monix-task` = (project in file("./sttp3-akka-monix-task"))
  .settings(global: _*)
  .settings(name := "sttp3-akka-monix-task", publishArtifact := true)
  .dependsOn(`sttp3-akka-monix-core`)

lazy val `sttp3-akka-monix-bio` = (project in file("./sttp3-akka-monix-bio"))
  .settings(global: _*)
  .settings(libraryDependencies ++= `monix-bio`)
  .settings(name := "sttp3-akka-monix-bio", publishArtifact := true)
  .dependsOn(`sttp3-akka-monix-core`)

lazy val root = (project in file("."))
  .settings(global: _*)
  .settings(publishArtifact := false)
  .aggregate(
    `sttp-akka-monix-core`,
    `sttp-akka-monix-task`,
    `sttp-akka-monix-bio`,
    `sttp3-akka-monix-core`,
    `sttp3-akka-monix-task`,
    `sttp3-akka-monix-bio`
  )