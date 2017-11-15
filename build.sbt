organization := "com.feedzai"

name := "cosy-test"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.11.12", "2.12.4")

scalacOptions ++= Seq(
  "-feature", "-deprecation",
  "-Xlint", "-Ywarn-unused-import", "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.scalatest" %% "scalatest" % "3.0.4" % Provided,
  "io.gatling" % "gatling-test-framework" % "2.2.5" % Provided
)
