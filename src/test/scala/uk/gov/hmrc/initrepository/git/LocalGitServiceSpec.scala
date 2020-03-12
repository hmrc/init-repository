/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.initrepository.git

import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

import scala.util.Try

class LocalGitServiceSpec extends WordSpec with Matchers with MockitoSugar {

  "LocalGitService" should {
    "format the readme for a private repository" in {
      new LocalGitService(mock[LocalGitStore]).buildReadmeTemplate(
        repoName    = "myRepo",
        privateRepo = true
      ) shouldBe
        """
          |# myRepo
          |
          |This is a placeholder README.md for a new repository
          |""".stripMargin
    }

    "format the readme for a public repository" in {
      new LocalGitService(mock[LocalGitStore]).buildReadmeTemplate(
        repoName    = "myRepo",
        privateRepo = false
      ) shouldBe
        s"""
           |# myRepo
           |
           |This is a placeholder README.md for a new repository
           |
           |### License
           |
           |This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
           |""".stripMargin
    }

  }

  "initialiseRepository" should {
    "initialise a private repository" in {
      val store   = mock[LocalGitStore]
      val service = new LocalGitService(store)

      when(store.cloneRepoURL(any[String])).thenReturn(Try((): Unit))
      when(store.commitFileToRoot(any[String], any[String], any[String], any[String], any[String]))
        .thenReturn(Try((): Unit))
      when(store.lastCommitSha(any[String])).thenReturn(Try(Some("abcd")))
      when(store.tagAnnotatedCommit(any[String], any[String], any[String], any[String])).thenReturn(Try((): Unit))
      when(store.push(any[String])).thenReturn(Try((): Unit))
      when(store.pushTags(any[String])).thenReturn(Try((): Unit))

      service.initialiseRepository(
        newRepoName        = "a-service",
        digitalServiceName = None,
        bootstrapTag       = Some("0.1.0"),
        privateRepo        = true,
        githubToken        = "token"
      )

      verify(store).cloneRepoURL(s"https://token@github.com/hmrc/a-service")
      verify(store).commitFileToRoot(
        "a-service",
        ".gitignore",
        service.gitIgnoreContents,
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "README.md",
        service.buildReadmeTemplate("a-service", true),
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "repository.yaml",
        "repoVisibility: private_12E5349CFB8BBA30AF464C24760B70343C0EAE9E9BD99156345DD0852C2E0F6F",
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).push("a-service")
      verify(store).tagAnnotatedCommit("a-service", "abcd", "Bootstrap tag", "0.1.0")
      verify(store).push("a-service")
      verify(store).pushTags("a-service")
    }

    "initialise a public repository" in {
      val store   = mock[LocalGitStore]
      val service = new LocalGitService(store)
      when(store.cloneRepoURL(any[String])).thenReturn(Try((): Unit))
      when(store.commitFileToRoot(any[String], any[String], any[String], any[String], any[String]))
        .thenReturn(Try((): Unit))
      when(store.lastCommitSha(any[String])).thenReturn(Try(Some("abcd")))
      when(store.tagAnnotatedCommit(any[String], any[String], any[String], any[String])).thenReturn(Try((): Unit))
      when(store.push(any[String])).thenReturn(Try((): Unit))
      when(store.pushTags(any[String])).thenReturn(Try((): Unit))

      service.initialiseRepository(
        newRepoName        = "a-service",
        digitalServiceName = None,
        bootstrapTag       = Some("0.1.0"),
        privateRepo        = false,
        githubToken        = "token"
      )

      verify(store).cloneRepoURL(s"https://token@github.com/hmrc/a-service")
      verify(store).commitFileToRoot(
        "a-service",
        ".gitignore",
        service.gitIgnoreContents,
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "README.md",
        service.buildReadmeTemplate("a-service", false),
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "repository.yaml",
        "repoVisibility: public_0C3F0CE3E6E6448FAD341E7BFA50FCD333E06A20CFF05FCACE61154DDBBADF71",
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).lastCommitSha("a-service")
      verify(store).tagAnnotatedCommit("a-service", "abcd", "Bootstrap tag", "0.1.0")
      verify(store).push("a-service")
      verify(store).pushTags("a-service")
    }

    "not require a bootstrap tag" in {
      val store   = mock[LocalGitStore]
      val service = new LocalGitService(store)
      when(store.cloneRepoURL(any[String])).thenReturn(Try((): Unit))
      when(store.commitFileToRoot(any[String], any[String], any[String], any[String], any[String]))
        .thenReturn(Try((): Unit))
      when(store.push(any[String])).thenReturn(Try((): Unit))

      service.initialiseRepository(
        newRepoName        = "a-service",
        digitalServiceName = None,
        bootstrapTag       = None,
        privateRepo        = false,
        githubToken        = "token"
      )

      verify(store).cloneRepoURL(s"https://token@github.com/hmrc/a-service")
      verify(store).commitFileToRoot(
        "a-service",
        ".gitignore",
        service.gitIgnoreContents,
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "README.md",
        service.buildReadmeTemplate("a-service", false),
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "repository.yaml",
        "repoVisibility: public_0C3F0CE3E6E6448FAD341E7BFA50FCD333E06A20CFF05FCACE61154DDBBADF71",
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).push("a-service")
      verifyNoMoreInteractions(store)
    }

    "initialise a private repository with digitalService name provided" in {
      val store   = mock[LocalGitStore]
      val service = new LocalGitService(store)

      when(store.cloneRepoURL(any[String])).thenReturn(Try((): Unit))
      when(store.commitFileToRoot(any[String], any[String], any[String], any[String], any[String]))
        .thenReturn(Try((): Unit))
      when(store.lastCommitSha(any[String])).thenReturn(Try(Some("abcd")))
      when(store.tagAnnotatedCommit(any[String], any[String], any[String], any[String])).thenReturn(Try((): Unit))
      when(store.push(any[String])).thenReturn(Try((): Unit))
      when(store.pushTags(any[String])).thenReturn(Try((): Unit))

      service.initialiseRepository(
        newRepoName        = "a-service",
        digitalServiceName = Some("test"),
        bootstrapTag       = Some("0.1.0"),
        privateRepo        = true,
        githubToken        = "token"
      )

      verify(store).cloneRepoURL(s"https://token@github.com/hmrc/a-service")
      verify(store).commitFileToRoot(
        "a-service",
        ".gitignore",
        service.gitIgnoreContents,
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "README.md",
        service.buildReadmeTemplate("a-service", true),
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).commitFileToRoot(
        "a-service",
        "repository.yaml",
        """repoVisibility: private_12E5349CFB8BBA30AF464C24760B70343C0EAE9E9BD99156345DD0852C2E0F6F
          |digital-service: test""".stripMargin,
        "hmrc-web-operations",
        "hmrc-web-operations@digital.hmrc.gov.uk")
      verify(store).push("a-service")
      verify(store).tagAnnotatedCommit("a-service", "abcd", "Bootstrap tag", "0.1.0")
      verify(store).push("a-service")
      verify(store).pushTags("a-service")
    }


  }


}
