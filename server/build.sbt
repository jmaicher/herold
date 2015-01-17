name := "herold-server"

version := "0.1"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "io.spray" %%  "spray-json" % "1.3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.8"
)
