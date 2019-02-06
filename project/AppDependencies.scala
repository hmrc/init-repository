import sbt._

object AppDependencies {

  val compile = Seq(
    "com.typesafe.play"         %% "play-ws"        % "2.5.19",
    "commons-io"                % "commons-io"      % "2.4",
    "com.github.scopt"          %% "scopt"          % "3.5.0",
    "org.apache.httpcomponents" % "httpcore"        % "4.3.2",
    "org.apache.httpcomponents" % "httpclient"      % "4.3.5",
    "ch.qos.logback"            % "logback-classic" % "1.2.3",
    "ch.qos.logback"            % "logback-core"    % "1.2.3"
  )
  val test = Seq(
    "org.scalatest"          %% "scalatest"  % "3.0.5" % "test",
    "org.pegdown"            % "pegdown"     % "1.6.0" % "test",
    "org.mockito"            % "mockito-all" % "1.10.19" % "test",
    "com.github.tomakehurst" % "wiremock"    % "1.58"  % "test"
  )
}
