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
                     repoName: String = "",
                     privateRepo: Boolean = false,
                     teamNames: Seq[String] = Nil,
                     verbose: Boolean = false,
                     digitalServiceName: Option[String] = None,
                     githubUsername: String        = "",
                     githubPassword: String        = ""
                   )


  val currentVersion = Option(getClass.getPackage.getImplementationVersion).getOrElse("(version not found)")

  val parser = new scopt.OptionParser[Config]("init-repository") {

    override def showUsageOnError = true

    head(s"\nInit-Repository", s"$currentVersion\n")

    help("help") text "prints this usage text"

    arg[String]("repo-name") action { (x, c) =>
      c.copy(repoName = x)
    } text "the name of the github repository"

    arg[Seq[String]]("team-names") action { (x, c) =>
      c.copy(teamNames = x.map(_.trim))
    } text "the github team name(s)"

    opt[Unit]("private") action { (x, c) =>
      c.copy(privateRepo = true)
    } text "creates a private repository. Default is public"

    opt[String]("digital-service-name").optional() action { (x, c) =>
      c.copy(digitalServiceName = Option(x))
    } text s"Digital service name"

    opt[String]("github-username") required () action { (x, c) =>
      c.copy(githubUsername = x)
    } text "github username"

    opt[String]("github-password") required () action { (x, c) =>
      c.copy(githubPassword = x)
    } text "github password"

    opt[Unit]("verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"
  }
}
