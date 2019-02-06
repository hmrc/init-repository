import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._

object AssemblySettings {
  def apply() = Seq(
    assemblyJarName in assembly := "init-repository.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
      case PathList("play", "core", "server", xs @ _*)              => MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties")     => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    artifact in (Compile, assembly) := {
      val art = (artifact in (Compile, assembly)).value
      art.copy(`classifier` = Some("assembly"))
    }
  )
}
