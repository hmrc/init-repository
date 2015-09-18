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
import scala.concurrent.ExecutionContext.Implicits.global

class Logger{
  def info(st:String) = println("[INFO] " + st)
  def debug(st:String) = println("[DEBUG] " + st)
}

object Main {

  val log = new Logger()

  def main(args: Array[String]) {
    val githubCredsFile  = System.getProperty("user.home") + "/.github/.credentials"
    val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

    val githubCredsOpt  = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
    val bintrayCredsOpt = CredentialsFinder.findBintrayCredsInFile(new File(bintrayCredsFile).toPath)

    val github  = new Github(new GithubHttp{
      override def creds: ServiceCredentials = githubCredsOpt.get
    }, new GithubUrls())

    val bintray = new Bintray(new BintrayHttp{
      override def creds: ServiceCredentials = githubCredsOpt.get
    }, new BintrayUrls())

    val newRepoName = "domain"
    val repo = "releases"
    
    github.containsRepo(newRepoName) map { containsRepo =>
      if(!containsRepo){
        bintray.containsPackage("releases", newRepoName) map { bintrayRepoExists =>
          if(!bintrayRepoExists){
            bintray.createPackage("releases", newRepoName)
            bintray.createPackage("release-candidates", newRepoName)
            github.createRepo(newRepoName)
          }
        }
      } else {
        new Logger().info("Repo already exists in github ")
      }
    }
  }

}
