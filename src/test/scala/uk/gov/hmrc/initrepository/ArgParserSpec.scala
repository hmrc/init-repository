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

      val args = Array("""repoName teamName Sbt 1.0.0 -v --enable-travis --private""".split(" "): _*)

      ArgParser.parser.parse(args, Config()).get shouldBe Config (
        repoName = "repoName",
        teamNames = Seq("teamName"),
        repoType = RepositoryType.Sbt,
        bootStrapTagName = "1.0.0",
        verbose = true,
        enableTravis = true,
        privateRepo = true
      )
    }

    "create config with default bootstrap version of 0.1.0 if none provided" in {


      var args = Array("""repoName teamName Sbt""".split(" "): _*)

      ArgParser.parser.parse(args, Config()).get shouldBe Config("repoName", Seq("teamName"), RepositoryType.Sbt, "0.1.0", false, false)
    }

    "create config by evaluating empty boot strap tag to default boot strap tag number" in {

      val args = Array("repoName","teamName", "Sbt"," ")

      ArgParser.parser.parse(args, Config()).get shouldBe Config("repoName", Seq("teamName"), RepositoryType.Sbt, "0.1.0", false, false)
    }

    "create config with multiple team names" in {

      val args = Array("repoName","teamName1, teamName2", "Sbt"," ")

      ArgParser.parser.parse(args, Config()).get shouldBe Config("repoName", Seq("teamName1", "teamName2"), RepositoryType.Sbt, "0.1.0", false, false)
    }

    "fail if correct boot strap version format is not provided" in {

      val args = Array("""repoName teamName Sbt v1.0.0""".split(" "): _*)

      ArgParser.parser.parse(args, Config()) shouldBe None
    }
  }

}
