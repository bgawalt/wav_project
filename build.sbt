name := "wav_project"

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.6",
  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test"
)