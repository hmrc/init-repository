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

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initrepository.ArgParser.Config
import uk.gov.hmrc.initrepository.RepositoryType.RepositoryType
import uk.gov.hmrc.initrepository.bintray._
import uk.gov.hmrc.initrepository.git.{LocalGitService, LocalGitStore}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object RepositoryType extends Enumeration {
  type RepositoryType = Value
  val Sbt, SbtPlugin = Value
}

object Main {

  def findGithubCreds(): ServiceCredentials = {
    val githubCredsFile = System.getProperty("user.home") + "/.github/.credentials"
    val githubCredsOpt = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
    val creds = githubCredsOpt.getOrElse(throw new scala.IllegalArgumentException(s"Did not find valid Github credentials in $githubCredsFile"))

    Log.debug(s"github client_id ${creds.user}")
    Log.debug(s"github client_secret ${creds.pass.takeRight(3)}*******")

    creds
  }

  def findTravisGithubCreds(): ServiceCredentials = {
    val travisGithubCredsFile = System.getProperty("user.home") + "/.github/.traviscredentials"
    val maybeCreds = CredentialsFinder.findGithubCredsInFile(new File(travisGithubCredsFile).toPath)
    val creds = maybeCreds.getOrElse(throw new scala.IllegalArgumentException(s"Did not find valid Travis Github credentials in $travisGithubCredsFile"))

    Log.debug(s"travis github client_id ${creds.user}")
    Log.debug(s"travis github client_secret ${creds.pass.takeRight(3)}*******")

    creds
  }

  def findBintrayCreds():ServiceCredentials={
    val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"
    val bintrayCredsOpt = CredentialsFinder.findBintrayCredsInFile(new File(bintrayCredsFile).toPath)

    val creds = bintrayCredsOpt.getOrElse(throw new IllegalArgumentException(s"Did not find valid Bintray credentials in $bintrayCredsFile"))

    Log.debug(s"bintrayCredsOpt client_id ${creds.user}")
    Log.debug(s"bintrayCredsOpt client_secret ${creds.pass.takeRight(3)}*******")

    creds
  }

  lazy val bintrayTransport = new HttpTransport {
    override val creds: ServiceCredentials = findBintrayCreds()
  }

  lazy val gitHubTransport = new HttpTransport {
    override val creds: ServiceCredentials = findGithubCreds()
  }

  lazy val travisTransport = new HttpTransport {
    override val creds: ServiceCredentials = findTravisGithubCreds()
  }

  def buildBintrayService(repositoryType:RepositoryType) = new BintrayService {
    override val bintray = new Bintray {
      override val http: HttpTransport = bintrayTransport
      override val urls: BintrayUrls = new BintrayUrls()
    }

    override val repositories: Set[String] = BintrayConfig(repositoryType)
  }

  def buildGithub() = new Github{
    override val httpTransport: HttpTransport = gitHubTransport
    override val githubUrls: GithubUrls = new GithubUrls()
  }

  def git = new LocalGitService(new LocalGitStore(Files.createTempDirectory("init-repository-git-store-")))

  def buildTravis = new TravisConnector {
    override def httpTransport: HttpTransport = travisTransport
    override def travisUrls: TravisUrls = new TravisUrls()
  }

  def main(args: Array[String]) {
      ArgParser.parser.parse(args, Config()).fold(throw new IllegalArgumentException("error while parsing provided arguments")) { config =>
        val root = LoggerFactory.getLogger(Log.loggerName).asInstanceOf[Logger]
        if(config.verbose) {
          root.setLevel(Level.DEBUG)
        } else {
          root.setLevel(Level.INFO)
        }
        start(config.repoName, config.teamNames, config.repoType, config.bootStrapTagName, config.enableTravis )
      }
  }

  def start(newRepoName: String, teams: Seq[String], repositoryType:RepositoryType, bootstrapVersion :String, enableTravis :Boolean): Unit = {
    val github = buildGithub()
    val bintray = buildBintrayService(repositoryType)
    val travis = buildTravis

    try {
      val result = new Coordinator(github, bintray, git, travis)
        .run(newRepoName, teams, repositoryType, bootstrapVersion, enableTravis)

      Await.result(result, Duration(120, TimeUnit.SECONDS))
    } finally {
      github.close()
      bintray.close()
      travis.close()
    }
  }
}
