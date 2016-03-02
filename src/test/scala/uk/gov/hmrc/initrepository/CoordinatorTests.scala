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

import uk.gov.hmrc.initrepository.bintray.BintrayService
import uk.gov.hmrc.initrepository.git.LocalGitService
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

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

      // setup pre-conditions
      when(github.teamId("teamname")) thenReturn Future.successful(Some(1))
      when(github.containsRepo("newrepo")) thenReturn FutureFalse
      when(bintray.reposContainingPackage("newrepo")) thenReturn Future.successful(Set[String]())

      // setup repo creation calls
      when(github.createRepo("newrepo")) thenReturn Future.successful("repo-url")
      when(bintray.createPackagesFor("newrepo")) thenReturn Future.successful()
      when(github.addRepoToTeam("newrepo", 1)) thenReturn Future.successful()
      when(github.createWebhook("newrepo", "webhookurl")) thenReturn Future.successful("webhookresource")

      // setup git calls
      when(git.initialiseRepository("repo-url", RepositoryType.Sbt)) thenReturn Success()

      new Coordinator(github, bintray, git).run("newrepo", "teamname", RepositoryType.Sbt, Some("webhookurl")).await

      // verify pre-conditions
      verify(github).containsRepo("newrepo")
      verify(github, atLeastOnce()).teamId("teamname")
      verify(bintray).reposContainingPackage("newrepo")

      // verify repo creation calls
      verify(github).createRepo("newrepo")
      verify(bintray).createPackagesFor("newrepo")
      verify(github).addRepoToTeam("newrepo", 1)
      verify(github).createWebhook("newrepo", "webhookurl")

    }
  }
}
