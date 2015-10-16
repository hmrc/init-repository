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

package uk.gov.hmrc.initrepository.bintray

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.initrepository.{FutureValues, WireMockEndpoints}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class BintrayServiceSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints with MockitoSugar{

  "BintrayService.reposContainingPackage" should {

    "not find any repositories containing a package" in {

      val mockBintray = mock[Bintray]

      when(mockBintray.containsPackage("releases", "newRepo")) thenReturn Future(false)
      when(mockBintray.containsPackage("release-candidates", "newRepo")) thenReturn Future(false)

      val bintrayService = new BintrayService{
        override val bintray: Bintray = mockBintray
        override val repositories = BintrayConfig.sbtStandardBintrayRepos.toSet
      }

      bintrayService.reposContainingPackage("newRepo").await shouldBe Set.empty[String]
    }

    "find repositories containing a package" in {

      val mockBintray = mock[Bintray]

      when(mockBintray.containsPackage("releases", "newRepo")) thenReturn Future.successful(true)
      when(mockBintray.containsPackage("release-candidates", "newRepo")) thenReturn Future.successful(true)

      val bintrayService = new BintrayService{
        override val bintray: Bintray = mockBintray
        override val repositories = BintrayConfig.sbtStandardBintrayRepos.toSet
      }

      bintrayService.reposContainingPackage("newRepo").await shouldBe Set("releases", "release-candidates")
    }

    "create a package" in {

      val mockBintray = mock[Bintray]

      when(mockBintray.createPackage("releases", "newRepo")) thenReturn Future.successful()
      when(mockBintray.createPackage("release-candidates", "newRepo")) thenReturn Future.successful()

      val bintrayService = new BintrayService{
        override val bintray: Bintray = mockBintray
        override val repositories = BintrayConfig.sbtStandardBintrayRepos.toSet
      }

      bintrayService.createPackagesFor("newRepo").await

      verify(mockBintray).createPackage("releases", "newRepo")
      verify(mockBintray).createPackage("release-candidates", "newRepo")
    }
  }
}
