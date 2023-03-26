Global / onChangedBuildSource := ReloadOnSourceChanges

val projectSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "nl.zwaaltjes",
  version      := "0.0.1-SNAPSHOT",
  scalaVersion := "3.2.2",

  // compiler settings
  javacOptions ++= Seq("-source", "11", "-target", "11"),
  javacOptions ++= Seq("-Xlint", "-Xlint:deprecation", "-encoding", "utf8"),
  scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8"),
  scalacOptions ++= Seq("-no-indent")
)

val dependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)

lazy val Quoridor = (project in file("."))
  .settings(name := "Quoridor")
  .settings(projectSettings)
  .settings(libraryDependencies ++= dependencies)
