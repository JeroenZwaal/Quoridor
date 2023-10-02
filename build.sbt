Global / onChangedBuildSource := ReloadOnSourceChanges

val projectSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "nl.zwaaltjes",
  version      := "0.0.1-SNAPSHOT",
  scalaVersion := "3.3.1",

  // compiler settings
  javacOptions ++= Seq("-source", "11", "-target", "11"),
  javacOptions ++= Seq("-Xlint", "-Xlint:deprecation", "-encoding", "utf8"),
  scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8"),
  scalacOptions ++= Seq("-no-indent")
)

val dependencies = Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.4",
  "com.typesafe.akka" %% "akka-stream" % "2.8.4",
  "com.typesafe.akka" %% "akka-http" % "10.5.2",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.2",
  "ch.qos.logback" % "logback-classic" % "1.2.12",

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
)

lazy val Quoridor = (project in file("."))
  .settings(name := "Quoridor")
  .settings(projectSettings)
  .settings(libraryDependencies ++= dependencies)
