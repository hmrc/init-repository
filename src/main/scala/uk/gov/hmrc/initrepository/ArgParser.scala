/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.initrepository

object ArgParser {

  case class Config(
    repository: String                 = "",
    isPrivate: Boolean                 = false,
    teams: Seq[String]                 = Nil,
    bootStrapTag: Option[String]       = None,
    verbose: Boolean                   = false,
    digitalServiceName: Option[String] = None,
    githubUsername: String             = "",
    githubToken: String                = ""
  )

  val currentVersion = Option(getClass.getPackage.getImplementationVersion).getOrElse("(version not found)")

  val parser = new scopt.OptionParser[Config]("init-repository") {

    override def showUsageOnError = true

    head(s"\nInit-Repository", s"$currentVersion\n")

    arg[String]("repository") action { (x, c) =>
      c.copy(repository = x)
    } text "the name of the github repository"

    help("help") text "prints this usage text"

    opt[Seq[String]]("teams") action { (x, c) =>
      c.copy(teams = x.map(_.trim))
    } text "the github team name(s)"

    opt[String]("bootstrap-tag").optional() action { (x, c) =>
      c.copy(bootStrapTag = Option(x.trim).filter(_.nonEmpty))
    } validate (x =>
      if (x.trim.isEmpty || x.matches("^\\d+.\\d+.\\d+$"))
        success
      else
        failure("Version number should be of correct format (i.e 1.0.0 , 0.10.1 etc).")) text "The bootstrap tag to kickstart release candidates. This should be 0.1.0 for *new* repositories or the most recent internal tag version for *migrated* repositories"

    opt[Unit]("private") action { (x, c) =>
      c.copy(isPrivate = true)
    } text "creates a private repository. Default is public"

    opt[String]("digital-service-name").optional() action { (x, c) =>
      c.copy(digitalServiceName = Option(x.trim).filter(_.nonEmpty))
    } text s"Digital service name"

    opt[String]("github-username") required () action { (x, c) =>
      c.copy(githubUsername = x)
    } text "github username"

    opt[String]("github-token") required () action { (x, c) =>
      c.copy(githubToken = x)
    } text "github token"

    opt[Unit]("verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"
  }
}
