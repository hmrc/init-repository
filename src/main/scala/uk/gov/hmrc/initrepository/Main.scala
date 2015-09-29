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

import git.LocalGitStore

import scala.concurrent.Await
import scala.concurrent.duration.Duration



object Main {

  val githubCredsFile  = System.getProperty("user.home") + "/.github/.credentials"
  val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

  val githubCredsOpt  = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
  val bintrayCredsOpt = CredentialsFinder.findBintrayCredsInFile(new File(bintrayCredsFile).toPath)

  if(githubCredsOpt.isEmpty) throw new IllegalArgumentException(s"Did not find valid Github credentials in ${githubCredsFile}")
  if(bintrayCredsOpt.isEmpty) throw new IllegalArgumentException(s"Did not find valid Bintray credentials in ${bintrayCredsFile}")

  private val githubHttp = new GithubHttp {
    override def creds: ServiceCredentials = githubCredsOpt.get

    Log.debug(s"github client_id ${creds.user}")
    Log.debug(s"github client_secret ${creds.pass.takeRight(3)}*******")

  }

  val github  = new Github{
    override def githubHttp: GithubHttp = githubHttp
    override def githubUrls: GithubUrls =  new GithubUrls()
  }
  val git = new LocalGitStore(Files.createTempDirectory("init-repository-git-store-"))
  
  private val bintrayHttp = new BintrayHttp {
    override def creds: ServiceCredentials = bintrayCredsOpt.get

    Log.debug(s"bintrayCredsOpt client_id ${creds.user}")
    Log.debug(s"bintrayCredsOpt client_secret ${creds.pass.takeRight(3)}*******")
  }

  val bintray = new Bintray{
    override def http: BintrayHttp = bintrayHttp
    override def urls: BintrayUrls = new BintrayUrls()
  }

  def main(args: Array[String]) {

    if(args.length != 2)
      throw new IllegalArgumentException("need 2 items")

    val newRepoName = args(0)
    val team = args(1)

    try {

      val coord = new Coordinator(github, bintray)
      val result = coord.run(newRepoName, team)

      Await.result(result, Duration(30, TimeUnit.SECONDS))
      Log.info("init-repository completed.")

    } finally {
      bintrayHttp.close()
      githubHttp.close()
    }
  }



}


