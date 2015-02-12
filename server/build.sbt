name := "herold-server"

version := "0.1"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.4.4",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "javax.websocket" % "javax.websocket-api" % "1.1",
  "javax.servlet" % "javax.servlet-api" % "3.1.0",
  "org.glassfish.grizzly" % "grizzly-http-server" % "2.3.18",
  "org.glassfish.grizzly" % "grizzly-websockets" % "2.3.18",
  "org.glassfish.grizzly" % "grizzly-http-servlet" % "2.3.18"
)

mainClass in assembly := Some("server.Server")

assemblyJarName in assembly := "server.jar"
