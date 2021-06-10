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
import uk.gov.hmrc.initrepository.ArgParser.Config

class ArgParserSpec extends WordSpec with Matchers {

  "ArgParser" should {
    "create correct config" in {

      val args = Array(
        """--verbose --private --github-username my-user --github-token my-pass --digital-service-name DSN --teams teamName --bootstrap-tag 1.0.0 repoName --require-signed-commits master --default-branch-name foo"""
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe Some(
        Config(
          repository         = "repoName",
          isPrivate          = true,
          teams              = Seq("teamName"),
          digitalServiceName = Some("DSN"),
          bootStrapTag       = Some("1.0.0"),
          verbose            = true,
          githubUsername     = "my-user",
          githubToken        = "my-pass",
          requireSignedCommits = Seq("master"),
          defaultBranchName = "foo"
        ))
    }

    "ignore optional params when empty values are provided" in {

      val args = Array(
        """--verbose --private --github-username my-user --github-token my-pass --default-branch-name foo --digital-service-name  --teams my-team --bootstrap-tag  repoName"""
          .split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe Some(
        Config(
          repository         = "repoName",
          isPrivate          = true,
          teams              = Seq("my-team"),
          digitalServiceName = None,
          bootStrapTag       = None,
          verbose            = true,
          githubUsername     = "my-user",
          githubToken        = "my-pass",
          requireSignedCommits = Seq.empty,
          defaultBranchName  = "foo"
        ))
    }

    "create config with multiple team names" in {

      val args = Array(
        """--github-username my-user --github-token my-pass --default-branch-name master --teams teamName1,teamName2 repoName""".split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe Some(
        Config(
          repository     = "repoName",
          isPrivate      = false,
          teams          = Seq("teamName1", "teamName2"),
          bootStrapTag   = None,
          githubUsername = "my-user",
          githubToken    = "my-pass",
          requireSignedCommits = Seq.empty,
          defaultBranchName = "master"
        ))
    }

    "create config with no team names" in {

      val args = Array(
        """--github-username my-user --github-token my-pass --default-branch-name main --teams  repoName""".split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe Some(
        Config(
          repository     = "repoName",
          isPrivate      = false,
          teams          = Seq.empty,
          bootStrapTag   = None,
          githubUsername = "my-user",
          githubToken    = "my-pass",
          requireSignedCommits = Seq.empty,
          defaultBranchName = "main"
        ))
    }

    "error when repository name is more than 47 characters" in {
      val mandatoryArgs = Array("""--github-username my-user --github-token my-pass --default-branch-name foo""".split(" "): _*)
      def repositoryName(length: Int) = List.fill(length)("x").mkString
      def parseRepositoryName(length: Int) = ArgParser.parser.parse(mandatoryArgs :+ repositoryName(length), Config())

      parseRepositoryName(length = 47) should be (defined)
      parseRepositoryName(length = 48) should not be defined
    }
  }

}
