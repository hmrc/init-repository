import sbtassembly.AssemblyKeys.assembly
import sbt._

val appName = "init-repository"

lazy val library = Project(appName, file("."))
  .settings(
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    AssemblySettings(),
    addArtifact(artifact in (Compile, assembly), assembly)
  )
