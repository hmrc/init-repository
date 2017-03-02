/*
 * Copyright 2016 HM Revenue & Customs
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

import uk.gov.hmrc.initrepository.RepositoryType.RepositoryType

object ArgParser {

  implicit val RepositoryTypeRead: scopt.Read[RepositoryType.Value] =
    scopt.Read.reads(RepositoryType withName _)

  val DEFAULT_BOOTSTRAP_TAG = "0.1.0"

  case class Config(
                     repoName: String = "",
                     teamNames: Seq[String] = Nil,
                     repoType: RepositoryType = RepositoryType.Sbt,
                     bootStrapTagName: String = DEFAULT_BOOTSTRAP_TAG,
                     verbose: Boolean = false,
                     enableTravis: Boolean = false
                   )


  val currentVersion = Option(getClass.getPackage.getImplementationVersion).getOrElse("(version not found)")

  val parser = new scopt.OptionParser[Config]("init-repository") {

    override def showUsageOnError = true

    head(s"\nInit-Repository", s"$currentVersion\n")

    help("help") text "prints this usage text"

    arg[String]("repo-name").abbr("rn") action { (x, c) =>
      c.copy(repoName = x)
    } text "the name of the github repository"

    arg[Seq[String]]("team-names").abbr("tns").valueName("<team1>,<team2>...") action { (x, c) =>
      c.copy(teamNames = x)
    } text "the github team names"

    arg[RepositoryType]("repository-type").abbr("rt") action { (x, c) =>
      c.copy(repoType = x)
    } text s"type of repository (${RepositoryType.values.map(_.toString).mkString(", ")}})"

    arg[String]("bootstrap-tag").abbr("bt") optional() action { (x, c) =>
      c.copy(bootStrapTagName = if (x.trim.isEmpty) DEFAULT_BOOTSTRAP_TAG else x)
    } validate (x =>
      if (x.trim.isEmpty || x.matches("^\\d+.\\d+.\\d+$"))
        success
      else
        failure("Version number should be of correct format (i.e 1.0.0 , 0.10.1 etc).")
      ) text "The bootstrap tag to kickstart release candidates. This should be 0.1.0 for *new* repositories or the most recent internal tag version for *migrated* repositories"

    opt[Unit]("enable-travis") action { (x, c) =>
      c.copy(enableTravis = true)
    } text "whether to enable travis integration"

    opt[Unit]('v', "verbose") action { (x, c) =>
      c.copy(verbose = true)
    } text "verbose mode (debug logging)"
  }
}
