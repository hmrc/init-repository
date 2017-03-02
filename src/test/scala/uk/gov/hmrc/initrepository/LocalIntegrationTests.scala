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

import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.scalatest.{Matchers, OptionValues, WordSpec}
import uk.gov.hmrc.initrepository.bintray.{Bintray, BintrayService}
import uk.gov.hmrc.initrepository.git.{LocalGitService, LocalGitStore}

import scala.concurrent.Future
import scala.util.Try

class LocalIntegrationTests extends WordSpec with Matchers with FutureValues with OptionValues{

  val bareOriginGitStoreDir = Files.createTempDirectory("local-origin-git-store-")

  "Coordinator.run" should {
    "result in a tag being pushed to a local git repository" in {

      val github = new Github {
        override def httpTransport: HttpTransport = ???
        override def githubUrls: GithubUrls = ???
        override def createRepo(repoName: String): Future[String] =
          Future.successful(s"${bareOriginGitStoreDir.toString}/$repoName")
        override def containsRepo(repoName: String): Future[Boolean] = Future.successful(false)
        override def teamId(team: String): Future[Option[Int]] = Future.successful(Some(1))
        override def addRepoToTeam(repoName: String, teamId: Int): Future[Unit] = Future.successful(Unit)
      }

      val bintray = new BintrayService {
        override def createPackagesFor(newPackageName:String):Future[Unit]= Future.successful(true)
        override def reposContainingPackage(newPackageName:String):Future[Set[String]]=Future.successful(Set())
        override lazy val repositories: Set[String] = ???
        override def bintray: Bintray = ???
      }

      val git = {
        val gitStore = Files.createTempDirectory("init-repository-git-store-")
        val localGitStore = new LocalGitStore(gitStore) {
          override def cloneRepoURL(url: String): Try[Unit] =
            GitRepoConfig.withNameConfig(this, getRepoNameFromUrl(url)) {
              super.cloneRepoURL(url)
            }

        }
        new LocalGitService(localGitStore)
      }

      val travis = new TravisConnector {
        override def httpTransport: HttpTransport = ???
        override def travisUrls: TravisUrls = ???

        override def authenticate: Future[TravisAuthenticationResult] = Future.successful(new TravisAuthenticationResult("access_token"))
        override def syncWithGithub(accessToken: String): Future[Unit] = Future.successful()
        override def searchForRepo(accessToken: String, repositoryName: String)(implicit backoffStrategy: TravisSearchBackoffStrategy) : Future[Int] = Future.successful(123456)
        override def activateHook(accessToken: String, repositoryId: Int): Future[Unit] = Future.successful()
      }

      val newRepoName = "test-repos"
      val origin = createOriginWithOneCommit(newRepoName)

      val coord = new Coordinator(github, bintray, git, travis)
      coord.run(newRepoName, Seq("un-used-in-this"), RepositoryType.SbtPlugin, "1.10.1", false).await

      origin.lastTag(newRepoName).get.value shouldBe "v1.10.1"
    }

    def createOriginWithOneCommit(newRepoName:String) = {
      val bareOriginGitStore = new LocalGitStore(bareOriginGitStoreDir)
      GitRepoConfig.withNameConfig(bareOriginGitStore, newRepoName) {
        bareOriginGitStore.init(newRepoName, isBare = true)
      }

      createACommit(bareOriginGitStoreDir.resolve(newRepoName).toString, newRepoName)
      bareOriginGitStore
    }

    def createACommit(bareRepoUrl:String, newRepoName:String): Unit ={
      val gitStore = Files.createTempDirectory("temporary-git-dir")
      val git = new LocalGitStore(gitStore)

      GitRepoConfig.withNameConfig(git, newRepoName) {
        git.cloneRepoURL(bareRepoUrl)
      }

      git.gitCommandParts(Array("config", "user.email", "'test@example.com'"), inRepo = Some(newRepoName)).map { _ => Unit }
      git.gitCommandParts(Array("config", "user.name", "'testUser'"), inRepo = Some(newRepoName)).map { _ => Unit }
      git.commitFileToRoot(newRepoName, "LICENCE", "the licence", "hmrc-web-operations", "e@ma.il")
      git.push(newRepoName)

      FileUtils.deleteDirectory(gitStore.toFile)

    }
  }

}

object GitRepoConfig {
  //to satisfy git on travis while running the tests
  def withNameConfig[T](store : LocalGitStore, reponame: String)(f : => T ) : T = {

    val result = f

    store.gitCommandParts(Array("config", "user.email", "'test@example.com'"), inRepo = Some(reponame)).map { _ => Unit }
    store.gitCommandParts(Array("config", "user.name", "'testUser'"), inRepo = Some(reponame)).map { _ => Unit }

    result
  }
}

