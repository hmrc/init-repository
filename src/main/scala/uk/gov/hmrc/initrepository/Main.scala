/*
 * Copyright 2015 HM Revenue & Customs
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
import uk.gov.hmrc.initrepository.bintray._
import uk.gov.hmrc.initrepository.git.{LocalGitService, LocalGitStore}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main {

  def buildBintrayService(isSbtPlugin:Boolean) = new BintrayService {

    val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

    val bintrayCredsOpt = CredentialsFinder.findBintrayCredsInFile(new File(bintrayCredsFile).toPath)

    if(bintrayCredsOpt.isEmpty) throw new IllegalArgumentException(s"Did not find valid Bintray credentials in ${bintrayCredsFile}")


    private val bintrayHttpImpl = new BintrayHttp {
      override val creds: ServiceCredentials = bintrayCredsOpt.get

      Log.debug(s"bintrayCredsOpt client_id ${creds.user}")
      Log.debug(s"bintrayCredsOpt client_secret ${creds.pass.takeRight(3)}*******")
    }

    override def bintray = new Bintray {
      override val http: BintrayHttp = bintrayHttpImpl
      override val urls: BintrayUrls = new BintrayUrls()
    }

    override val repositories: Set[String] = BintrayConfig(isSbtPlugin)
  }

  def buildGithub() = new Github{

    val githubCredsFile  = System.getProperty("user.home") + "/.github/.credentials"
    val githubCredsOpt  = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
    if(githubCredsOpt.isEmpty) throw new IllegalArgumentException(s"Did not find valid Github credentials in ${githubCredsFile}")

    private val githubHttpImpl = new GithubHttp {
      override val creds: ServiceCredentials = githubCredsOpt.get

      Log.debug(s"github client_id ${creds.user}")
      Log.debug(s"github client_secret ${creds.pass.takeRight(3)}*******")

    }

    override val githubHttp: GithubHttp = githubHttpImpl
    override val githubUrls: GithubUrls =  new GithubUrls()

  }

  def git = new LocalGitService(new LocalGitStore(Files.createTempDirectory("init-repository-git-store-")))


  def main(args: Array[String]) {

      ArgParser.parser.parse(args, Config()) foreach { config =>
        if(config.verbose) {
          val root = LoggerFactory.getLogger(Log.loggerName).asInstanceOf[Logger]
          root.setLevel(Level.DEBUG)
        }
        start(config.repoName, config.teamName, config.isSbtPlugin)
      }

  }

  def start(newRepoName: String, team: String, isSbtPlugin: Boolean): Unit = {

    val github = buildGithub()
    val bintray = buildBintrayService(isSbtPlugin)

    try {
      val result = new Coordinator(github, bintray, git)
        .run(newRepoName, team)

      Await.result(result, Duration(60, TimeUnit.SECONDS))
    } finally {
      github.close()
      bintray.close()
    }
  }
}


