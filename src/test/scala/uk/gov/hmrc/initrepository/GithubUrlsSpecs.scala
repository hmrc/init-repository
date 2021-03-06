/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{Matchers, WordSpec}

class GithubUrlsSpecs extends WordSpec with Matchers {

  "GithubUrls.containsRepo" should {
    "generate correct repo url " in {
      new GithubUrls().containsRepo("domain").toString shouldBe
        "https://api.github.com/repos/hmrc/domain"
    }

    "generate correct create repo url " in {
      new GithubUrls().createRepo.toString shouldBe
        "https://api.github.com/orgs/hmrc/repos"
    }
  }

  "GithubUrls enable branch protection" should {
    "generate the correct repo URL" in {
      new GithubUrls().addBranchProtection("some-repo", "master").toString shouldBe
        "https://api.github.com/repos/hmrc/some-repo/branches/master/protection"
    }
  }

  "GithubUrls add branch protection to require signed commits" should {
    "generate the correct repo URL" in {
      new GithubUrls().addRequireSignedCommits("some-repo", "master").toString shouldBe
        "https://api.github.com/repos/hmrc/some-repo/branches/master/protection/required_signatures"
    }
  }

}
