package uk.gov.hmrc.initrepository.bintray

import uk.gov.hmrc.initrepository.RepositoryType


object BintrayConfig{

  import RepositoryType._

  private[bintray] val sbtPluginBintrayRepos = Set("sbt-plugin-releases", "sbt-plugin-release-candidates")
  private[bintray] val sbtStandardBintrayRepos = Set("releases", "release-candidates")

  def apply(repositoryType:RepositoryType):Set[String]={
    repositoryType match {
      case SbtPlugin => BintrayConfig.sbtPluginBintrayRepos
      case Sbt       => BintrayConfig.sbtStandardBintrayRepos
    }
  }
}
