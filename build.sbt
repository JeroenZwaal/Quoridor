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
  "org.apache.pekko" %% "pekko-actor-typed" % "1.0.1",
  "org.apache.pekko" %% "pekko-stream" % "1.0.1",
  "org.apache.pekko" %% "pekko-http" % "1.0.0",
  "org.apache.pekko" %% "pekko-http-spray-json" % "1.0.0",
  // "com.github.pjfanning" %% "pekko-http-session-core" % "0.8.0",
  "ch.qos.logback" % "logback-classic" % "1.2.12",

  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
)

lazy val Quoridor = (project in file("."))
  .settings(name := "Quoridor")
  .settings(projectSettings)
  .settings(libraryDependencies ++= dependencies)
