package uk.gov.hmrc.initrepository.bintray


object BintrayConfig{

  private[bintray] val sbtPluginBintrayRepos = Set("sbt-plugin-releases", "sbt-plugin-release-candidates")
  private[bintray] val sbtStandardBintrayRepos = Set("releases", "release-candidates")

  def apply(isSbtPluginRepo:Boolean):Set[String]={
    isSbtPluginRepo match {
      case true  => BintrayConfig.sbtPluginBintrayRepos
      case false => BintrayConfig.sbtStandardBintrayRepos
    }
  }
}
