import sbtassembly.AssemblyKeys.assembly
import sbt._

val appName = "init-repository"

lazy val library = Project(appName, file("."))
  .settings(
    isPublicArtefact := true,
    majorVersion := 1,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    assembly / assemblyJarName := "init-repository.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    addArtifact(artifact in (Compile, assembly), assembly)
  )
