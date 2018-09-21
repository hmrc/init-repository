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

import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import uk.gov.hmrc.initrepository.git.LocalGitService

import scala.concurrent.Future
import scala.util.Success

class CoordinatorSpec extends WordSpec with Matchers with ScalaFutures with IntegrationPatience with BeforeAndAfterEach with MockitoSugar {

  val FutureFalse = Future.successful(false)
  val FutureUnit = Future.successful(Unit)

  val digitalServiceName = Some("digital-service-123")

  "Coordinator.run" should {
    "run operations in order when calls are successful" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      val repoName = "newrepo"
      val repoId = 2364862
      val teamName: String = "teamname"
      val repoUrl = "repo-url"
      val bootstrapTag = "1.0.0"

      // setup pre-conditions
      when(github.teamId(teamName)) thenReturn Future.successful(Some(1))
      when(github.teamId("Repository Admins")) thenReturn Future.successful(Some(10))
      when(github.containsRepo(repoName)) thenReturn FutureFalse

      // setup repo creation calls
      when(github.createRepo(repoName, privateRepo = false)) thenReturn Future.successful(repoUrl)
      when(github.addRepoToTeam(repoName, 1, "push")) thenReturn Future.successful(())
      when(github.addRepoToTeam(repoName, 10, "admin")) thenReturn Future.successful(())

      // setup git calls
      when(git.initialiseRepository(repoUrl, digitalServiceName, privateRepo = false)) thenReturn Success(())

      // setup travis calls
      val accessToken = "access_token"

      new Coordinator(github, git).run(repoName, Seq(teamName), digitalServiceName, privateRepo = false).futureValue

      // verify pre-conditions
      verify(github).containsRepo(repoName)
      verify(github, atLeastOnce()).teamId(teamName)

      // verify repo creation calls
      verify(github).createRepo(repoName, privateRepo = false)
      verify(github).addRepoToTeam(repoName, 1, "push")

    }
    "adds multiple teams to the new repo, including the Repository Admins team" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      val repoName = "newrepo"
      val repoId = 2364862
      val teamName1: String = "teamname"
      val teamName2: String = "Designers"
      val repoUrl = "repo-url"
      val bootstrapTag = "1.0.0"

      // setup pre-conditions
      when(github.teamId(teamName1)) thenReturn Future.successful(Some(1))
      when(github.teamId(teamName2)) thenReturn Future.successful(Some(2))
      when(github.teamId("Repository Admins")) thenReturn Future.successful(Some(10))
      when(github.containsRepo(repoName)) thenReturn FutureFalse

      // setup repo creation calls
      when(github.createRepo(repoName, privateRepo = false)) thenReturn Future.successful(repoUrl)
      when(github.addRepoToTeam(repoName, 1, "push")) thenReturn Future.successful(())
      when(github.addRepoToTeam(repoName, 2, "push")) thenReturn Future.successful(())
      when(github.addRepoToTeam(repoName, 10, "admin")) thenReturn Future.successful(())

      // setup git calls
      when(git.initialiseRepository(repoUrl, digitalServiceName, privateRepo = false)) thenReturn Success(())

      // setup travis calls
      val accessToken = "access_token"


      new Coordinator(github, git).run(repoName,  Seq(teamName1, teamName2), digitalServiceName, privateRepo = false).futureValue

      // verify pre-conditions
      verify(github).containsRepo(repoName)
      verify(github, atLeastOnce()).teamId(teamName1)
      verify(github, atLeastOnce()).teamId(teamName2)

      // verify repo creation calls
      verify(github).createRepo(repoName, privateRepo = false)
      verify(github).addRepoToTeam(repoName, 1, "push")
      verify(github).addRepoToTeam(repoName, 2, "push")
      verify(github).addRepoToTeam(repoName, 10, "admin")

    }
    "run operations without enabling travis if enableTravis is false" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      val repoName = "newrepo"
      val repoId = 2364862
      val teamName: String = "teamname"
      val repoUrl = "repo-url"
      val bootstrapTag = "1.0.0"

      // setup pre-conditions
      when(github.teamId(teamName)) thenReturn Future.successful(Some(1))
      when(github.teamId("Repository Admins")) thenReturn Future.successful(Some(10))
      when(github.containsRepo(repoName)) thenReturn FutureFalse

      // setup repo creation calls
      when(github.createRepo(repoName, privateRepo = false)) thenReturn Future.successful(repoUrl)
      when(github.addRepoToTeam(repoName, 1, "push")) thenReturn Future.successful(())
      when(github.addRepoToTeam(repoName, 10, "admin")) thenReturn Future.successful(())

      // setup git calls
      when(git.initialiseRepository(repoUrl, digitalServiceName, privateRepo = false)) thenReturn Success(())


      new Coordinator(github, git).run(repoName,  Seq(teamName), digitalServiceName, privateRepo = false).futureValue

      // verify pre-conditions
      verify(github).containsRepo(repoName)
      verify(github, atLeastOnce()).teamId(teamName)

      // verify repo creation calls
      verify(github).createRepo(repoName, privateRepo = false)
      verify(github).addRepoToTeam(repoName, 1, "push")

    }

    "creates a private repository and does not integrate with travis and Bintray" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      val repoName = "newrepo"
      val repoId = 2364862
      val teamName: String = "teamname"
      val repoUrl = "repo-url"
      val bootstrapTag = "1.0.0"
      val privateRepo = true

      // setup pre-conditions
      when(github.teamId(teamName)) thenReturn Future.successful(Some(1))
      when(github.teamId("Repository Admins")) thenReturn Future.successful(Some(10))
      when(github.containsRepo(repoName)) thenReturn FutureFalse

      // setup repo creation calls
      when(github.createRepo(repoName, privateRepo = true)) thenReturn Future.successful(repoUrl)
      when(github.addRepoToTeam(repoName, 1, "push")) thenReturn Future.successful(())
      when(github.addRepoToTeam(repoName, 10, "admin")) thenReturn Future.successful(())

      // setup git calls
      when(git.initialiseRepository(repoUrl, digitalServiceName, privateRepo)) thenReturn Success(())


      new Coordinator(github, git).run(
        newRepoName = repoName,
        teams =  Seq(teamName),
        digitalServiceName = digitalServiceName,
        privateRepo = privateRepo
      ).futureValue

      // verify pre-conditions
      verify(github).containsRepo(repoName)
      verify(github, atLeastOnce()).teamId(teamName)

      // verify repo creation calls
      verify(github).createRepo(repoName, privateRepo = true)
      verify(github).addRepoToTeam(repoName, 1, "push")

    }

  }

  "checkTeamsExistOnGithub" should {
    "return true if all provided teams exist on github" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      when(github.teamId("team1")) thenReturn Future.successful(Some(1))
      when(github.teamId("team2")) thenReturn Future.successful(Some(2))

      new Coordinator(github, git).checkTeamsExistOnGithub(Seq("team1", "team2")).futureValue shouldBe true
    }

    "return false if at least one of the provided teams does not exist on github" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      when(github.teamId("team1")) thenReturn Future.successful(Some(1))
      when(github.teamId("team2")) thenReturn Future.successful(None)

      new Coordinator(github, git).checkTeamsExistOnGithub(Seq("team1", "team2")).futureValue shouldBe false
    }
    "return false if all  of the provided teams do not exist on github" in {

      val github = mock[Github]
      val git = mock[LocalGitService]

      when(github.teamId("team1")) thenReturn Future.successful(None)
      when(github.teamId("team2")) thenReturn Future.successful(None)

      new Coordinator(github, git).checkTeamsExistOnGithub(Seq("team1", "team2")).futureValue shouldBe false
    }
  }
}
