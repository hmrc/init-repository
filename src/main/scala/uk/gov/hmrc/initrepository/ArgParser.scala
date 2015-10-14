package uk.gov.hmrc.initrepository

import uk.gov.hmrc.initrepository.RepositoryType.RepositoryType

object ArgParser{

  implicit val RepositoryTypeRead: scopt.Read[RepositoryType.Value] =
    scopt.Read.reads(RepositoryType withName _)

  case class Config(
                     repoName: String = "",
                     teamName:String = "",
                     repoType:RepositoryType = RepositoryType.Sbt,
                     verbose:Boolean = false)

  val currentVersion = Option(getClass.getPackage.getImplementationVersion).getOrElse("(version not found)")

  val parser = new scopt.OptionParser[Config]("init-repository") {

    override def showUsageOnError = true

    head(s"\nInit-Repository", s"$currentVersion\n")

    help("help") text "prints this usage text"

    arg[String]("repo-name") action { (x, c) =>
      c.copy(repoName = x) } text "the name of the github repository"

    arg[String]("team-name") action { (x, c) =>
      c.copy(teamName = x) } text "the github team name"

    arg[RepositoryType]("repository-type") action { (x, c) =>
      c.copy(repoType = x) } text s"type of repository (${RepositoryType.values.map(_.toString).mkString(", ")}})"

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true) } text "verbose mode (debug logging)"
  }
}



