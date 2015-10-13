package uk.gov.hmrc.initrepository

object ArgParser{

  case class Config(
                     repoName: String = "",
                     teamName:String = "",
                     isSbtPlugin:Boolean = false,
                     verbose:Boolean = false)

  val currentVersion = Option(getClass.getPackage.getImplementationVersion).getOrElse("(version not found)")

  val parser = new scopt.OptionParser[Config]("init-repository") {
    override def showUsageOnError = false
    head(s"\nInit-Repository", s"$currentVersion\n")
    help("help") text "prints this usage text"
    arg[String]("repo-name") action { (x, c) =>
      c.copy(repoName = x) } text "the name of the github repository"
    arg[String]("team-name") action { (x, c) =>
      c.copy(teamName = x) } text "the github team name"
    opt[Unit]('s', "sbt-plugin") action { (x, c) =>
      c.copy(isSbtPlugin = true) } text "create sbt-plugin repositories in Bintray"
    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true) } text "verbose mode (debug logging)"
  }
}
