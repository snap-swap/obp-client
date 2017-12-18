name := "obp-client"

organization := "com.snapswap"

version := "0.0.3"

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Ywarn-unused-import",
  "-encoding",
  "UTF-8"
)


libraryDependencies ++= {
  val akkaV = "2.4.18"
  val akkaHttpV = "10.0.7"

  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.google.code.findbugs" % "jsr305" % "3.0.1" % "provided",
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.google.guava" % "guava" % "22.0",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test"
  )
}
