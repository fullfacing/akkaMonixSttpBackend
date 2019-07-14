import xerial.sbt.Sonatype._

// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "com.fullfacing"

// Sonatype Nexus Credentials
credentials += Credentials(Path.userHome / ".sbt" / "1.0" / ".credentials")

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// MIT Licence
licenses  := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

// Github Project Information
sonatypeProjectHosting := Some(GitHubHosting("fullfacing", "sttp-akka-monix", "curious@fullfacing.com"))

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
  )
)