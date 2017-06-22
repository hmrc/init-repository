/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.initrepository.RepositoryType

class LocalGitServiceSpec extends WordSpec with Matchers with MockitoSugar {
  "LocalGitService" should {
    "format the readme for a private repository" in {
      new LocalGitService(mock[LocalGitStore]).buildReadmeTemplate(
        repoName = "myRepo",
        repositoryType = RepositoryType.Sbt,
        enableTravis = false,
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
        repoName = "myRepo",
        repositoryType = RepositoryType.Sbt,
        enableTravis = false,
        privateRepo = false
      ) shouldBe
        s"""
           |# myRepo
           |
           | [ ![Download](https://api.bintray.com/packages/hmrc/releases/myRepo/images/download.svg) ](https://bintray.com/hmrc/releases/myRepo/_latestVersion)
           |
           |This is a placeholder README.md for a new repository
           |
           |### License
           |
           |This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
           |""".stripMargin
    }

    "format the readme for a public repository integrated with travis" in {
      new LocalGitService(mock[LocalGitStore]).buildReadmeTemplate(
        repoName = "myRepo",
        repositoryType = RepositoryType.Sbt,
        enableTravis = true,
        privateRepo = false
      ) shouldBe
        s"""
           |# myRepo
           |
           |[![Build Status](https://travis-ci.org/hmrc/myRepo.svg?branch=master)](https://travis-ci.org/hmrc/myRepo) [ ![Download](https://api.bintray.com/packages/hmrc/releases/myRepo/images/download.svg) ](https://bintray.com/hmrc/releases/myRepo/_latestVersion)
           |
           |This is a placeholder README.md for a new repository
           |
           |### License
           |
           |This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
           |""".stripMargin
    }
  }
}
