name := "herold-server"

version := "0.1"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "io.spray" %%  "spray-json" % "1.3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.8",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0",
  "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % "2.5.0"
)
