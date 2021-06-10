import sbtassembly.AssemblyKeys.assembly
import sbt._

val appName = "init-repository"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
  .settings(
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    AssemblySettings(),
    addArtifact(artifact in (Compile, assembly), assembly)
  )
