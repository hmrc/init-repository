
import sbt._


object AppDependencies {

  val compile = Seq(
    "com.typesafe.play" %% "play-ws" % "2.4.3",
    "commons-io" % "commons-io" % "2.4",
    "com.github.scopt" %% "scopt" % "3.5.0",
    "org.apache.httpcomponents" % "httpcore" % "4.3.2",
    "org.apache.httpcomponents" % "httpclient" % "4.3.5"
  )
  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.2" % "test",
    "org.pegdown" % "pegdown" % "1.4.2" % "test",
    "org.mockito" % "mockito-all" % "1.9.5" % "test",
    "com.github.tomakehurst" % "wiremock" % "1.58" % "test"
  )
}