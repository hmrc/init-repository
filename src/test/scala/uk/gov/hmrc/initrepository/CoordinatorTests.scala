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

import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import uk.gov.hmrc.initrepository.bintray.BintrayService
import uk.gov.hmrc.initrepository.git.LocalGitService

import scala.concurrent.Future
import scala.util.Success

class CoordinatorTests extends WordSpec with Matchers with FutureValues with BeforeAndAfterEach with MockitoSugar {

  val FutureFalse = Future.successful(false)
  val FutureUnit = Future.successful(Unit)

  "Coordinator.run" should {
    "run operations in order when calls are successful" in {

      val github = mock[Github]
      val bintray = mock[BintrayService]
      val git = mock[LocalGitService]
      val travis = mock[TravisConnector]

      val repoName = "newrepo"
      val repoId = 2364862
      val teamName1: String = "teamname1"
      val teamName2: String = "teamname2"
      val teamId1: Int = 1
      val teamId2: Int = 2
      val repoUrl = "repo-url"
      val bootstrapTag = "1.0.0"

      // setup pre-conditions
      when(github.teamId(teamName1)) thenReturn Future.successful(Some(teamId1))
      when(github.teamId(teamName2)) thenReturn Future.successful(Some(teamId2))
      when(github.containsRepo(repoName)) thenReturn FutureFalse
      when(bintray.reposContainingPackage(repoName)) thenReturn Future.successful(Set[String]())

      // setup repo creation calls
      when(github.createRepo(repoName)) thenReturn Future.successful(repoUrl)
      when(bintray.createPackagesFor(repoName)) thenReturn Future.successful()
      when(github.addRepoToTeam(repoName, teamId1)) thenReturn Future.successful()
      when(github.addRepoToTeam(repoName, teamId2)) thenReturn Future.successful()

      // setup git calls
      when(git.initialiseRepository(repoUrl, RepositoryType.Sbt, bootstrapTag)) thenReturn Success()

      // setup travis calls
      val accessToken = "access_token"

      implicit val backoffStrategy = TravisSearchBackoffStrategy(1, 0)

      when(travis.authenticate) thenReturn Future.successful(new TravisAuthenticationResult(accessToken))
      when(travis.syncWithGithub(accessToken)) thenReturn Future.successful()
      when(travis.searchForRepo(meq(accessToken), meq(repoName))(any())) thenReturn Future.successful(repoId)
      when(travis.activateHook(accessToken, repoId)) thenReturn Future.successful()

      new Coordinator(github, bintray, git, travis).run(repoName, Seq(teamName1, teamName2), RepositoryType.Sbt, bootstrapTag, enableTravis = true).await

      // verify pre-conditions
      verify(github).containsRepo(repoName)
      verify(github, atLeastOnce()).teamId(teamName1)
      verify(bintray).reposContainingPackage(repoName)

      // verify repo creation calls
      verify(github).createRepo(repoName)
      verify(bintray).createPackagesFor(repoName)
      verify(github).addRepoToTeam(repoName, teamId1)
      verify(github).addRepoToTeam(repoName, teamId2)

      // verify travis setup
      verify(travis).authenticate
      verify(travis).syncWithGithub(accessToken)
      verify(travis).searchForRepo(meq(accessToken), meq(repoName))(any())
      verify(travis).activateHook(accessToken, repoId)

    }
    "run operations without enabling travis if enableTravis is false" in {

      val github = mock[Github]
      val bintray = mock[BintrayService]
      val git = mock[LocalGitService]
      val travis = mock[TravisConnector]

      val repoName = "newrepo"
      val repoId = 2364862
      val teamName: String = "teamname"
      val repoUrl = "repo-url"
      val bootstrapTag = "1.0.0"

      // setup pre-conditions
      when(github.teamId(teamName)) thenReturn Future.successful(Some(1))
      when(github.containsRepo(repoName)) thenReturn FutureFalse
      when(bintray.reposContainingPackage(repoName)) thenReturn Future.successful(Set[String]())

      // setup repo creation calls
      when(github.createRepo(repoName)) thenReturn Future.successful(repoUrl)
      when(bintray.createPackagesFor(repoName)) thenReturn Future.successful()
      when(github.addRepoToTeam(repoName, 1)) thenReturn Future.successful()

      // setup git calls
      when(git.initialiseRepository(repoUrl, RepositoryType.Sbt, bootstrapTag)) thenReturn Success()


      new Coordinator(github, bintray, git, travis).run(repoName, Seq(teamName), RepositoryType.Sbt, bootstrapTag, enableTravis = false).await

      // verify pre-conditions
      verify(github).containsRepo(repoName)
      verify(github, atLeastOnce()).teamId(teamName)
      verify(bintray).reposContainingPackage(repoName)

      // verify repo creation calls
      verify(github).createRepo(repoName)
      verify(bintray).createPackagesFor(repoName)
      verify(github).addRepoToTeam(repoName, 1)

      // verify no travis
      verifyZeroInteractions(travis)


    }

  }
}

