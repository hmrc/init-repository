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

import java.io.{File, PrintWriter}

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}


class ToolsSupportSpec extends WordSpec with Matchers with BeforeAndAfterEach {


  override protected def beforeEach(): Unit = {

    new File(System.getProperty("java.io.tmpdir") + "/.github").mkdir()
    new File(System.getProperty("java.io.tmpdir") + "/.github/.credentials").delete()

    new File(System.getProperty("java.io.tmpdir") + "/.github.com").mkdir()
    new File(System.getProperty("java.io.tmpdir") + "/.github.com/.credentials").delete()

  }

  "ToolsSupport" should {

    val main = new ToolsSupport {
      override lazy val homeFolder: String = System.getProperty("java.io.tmpdir")
    }

    "read a github credentials file" in {

      new PrintWriter(System.getProperty("java.io.tmpdir") + "/.github/.credentials") { write("token=mySecretTokenOnGithub"); close() }

      main.findGithubCreds() shouldBe ServiceCredentials("token", "mySecretTokenOnGithub")

    }

    "read from ~/.github.com/.credentials first" in {

      new PrintWriter(System.getProperty("java.io.tmpdir") + "/.github.com/.credentials") { write("token=mySecretTokenOnGithub.com"); close() }
      new PrintWriter(System.getProperty("java.io.tmpdir") + "/.github/.credentials") { write("token=mySecretTokenOnGithub"); close() }

      main.findGithubCreds() shouldBe ServiceCredentials("token", "mySecretTokenOnGithub.com")

    }

    "throw an exception if github credentials do not exist" in {
      intercept[IllegalArgumentException] {
        main.findGithubCreds()
      }.getMessage shouldBe "Did not find valid Github credentials in ~/.github/.credentials or ~/.github.com/.credentials"
    }
  }

}
