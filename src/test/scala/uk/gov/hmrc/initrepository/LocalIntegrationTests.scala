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

import java.nio.file.{Path, Files}

import uk.gov.hmrc.initrepository.git.{LocalGitService, LocalGitStore}
import org.apache.commons.io.FileUtils
import org.scalatest.{Matchers, OptionValues, WordSpec}

import scala.concurrent.Future

class LocalIntegrationTests extends WordSpec with Matchers with FutureValues with OptionValues{

  val bareOriginGitStoreDir = Files.createTempDirectory("local-origin-git-store-")

  "Coordinator.run" should {
    "result in a tag being pushed to a local git repository" in {

      val github = new Github {

        override def githubHttp: GithubHttp = ???

        override def githubUrls: GithubUrls = ???

        override def createRepo(repoName: String): Future[String] = Future.successful(s"${bareOriginGitStoreDir.toString}/$repoName")

        override def containsRepo(repoName: String): Future[Boolean] = Future.successful(false)

        override def teamId(team: String): Future[Option[Int]] = Future.successful(Some(1))

        override def addRepoToTeam(repoName: String, teamId: Int): Future[Unit] = Future.successful(Unit)
      }

      val bintray = new Bintray {

        override def urls: BintrayUrls = ???

        override def http: BintrayHttp = ???

        override def containsPackage(repoName: String, packageName: String): Future[Boolean] = Future.successful(false)

        override def createPackage(repoName: String, packageName: String): Future[Unit] = Future.successful(Unit)
      }

      val git = {
        val gitStore = Files.createTempDirectory("init-repository-git-store-")
        val localGitStore = new LocalGitStore(gitStore)
        new LocalGitService(localGitStore)
      }

      val newRepoName = "test-repos"

      val origin = createOriginWithOneCommit(newRepoName)

      val coord = new Coordinator(github, bintray, git)
      coord.run(newRepoName, team = "un-used-in-this").await

      origin.lastTag(newRepoName).await.value shouldBe "v0.1.0"
    }

    def createOriginWithOneCommit(newRepoName:String) = {
      val bareOriginGitStore = new LocalGitStore(bareOriginGitStoreDir)
      bareOriginGitStore.init(newRepoName, isBare = true).await
      createACommit(bareOriginGitStoreDir.resolve(newRepoName).toString, newRepoName)
      bareOriginGitStore
    }


    def createACommit(bareRepoUrl:String, newRepoName:String): Unit ={
      val gitStore = Files.createTempDirectory("temporary-git-dir")
      val git = new LocalGitStore(gitStore)

      git.cloneRepoURL(bareRepoUrl).await
      git.commitFileToRoot(newRepoName, "LICENCE", "the licence", "hmrc-web-operations", "e@ma.il").await
      git.push(newRepoName).await

      FileUtils.deleteDirectory(gitStore.toFile)

    }
  }
}
