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

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.initrepository.ArgParser.Config

class ArgParserSpec extends WordSpec with Matchers {


  "ArgParser" should {
    "create correct config" in {

      val args = Array("""--verbose --private --github-username my-user --github-password my-pass repoName teamName""".split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe Some(Config(
        repoName = "repoName",
        privateRepo = true,
        teamNames = Seq("teamName"),
        verbose = true,
        githubUsername = "my-user",
        githubPassword = "my-pass"
      ))
    }

    "create config with multiple team names" in {

      val args = Array("""--github-username my-user --github-password my-pass repoName teamName1,teamName2""".split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe Some(Config(
        repoName = "repoName",
        privateRepo = false,
        teamNames = Seq("teamName1", "teamName2"),
        githubUsername = "my-user",
        githubPassword = "my-pass"
      ))
    }
  }

}
