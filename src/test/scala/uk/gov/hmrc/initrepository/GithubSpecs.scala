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

import java.net.URL

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import com.ning.http.util.Base64
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import uk.gov.hmrc.initrepository.wiremock.{GithubWireMocks, WireMockEndpoints}

import scala.concurrent.Await
import scala.concurrent.duration._

class GithubSpecs
    extends WordSpec
    with Matchers
    with ScalaFutures
    with WireMockEndpoints
    with GithubWireMocks
    with IntegrationPatience {

  val basicAuthHeader = "Basic " + Base64.encode("my-user:my-token".getBytes())
  val transport       = new HttpTransport("my-user", "my-token")

  val github: Github = new Github {
    override def httpTransport: HttpTransport = transport
    override def githubUrls: GithubUrls       = urls
  }

  val urls     = new GithubUrls(apiRoot = endpointMockUrl)
  val repoName = "domain"

  "Github.containsRepo" should {

    "return true when github returns 200" in {

      givenGitHubExpects(
        method          = GET,
        url             = urls.containsRepo(repoName),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (200, None)
      )

      github.containsRepo(repoName).futureValue shouldBe true
    }

    "return false when github returns 404" in {

      givenGitHubExpects(
        method          = GET,
        url             = urls.containsRepo(repoName),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (404, None)
      )

      github.containsRepo(repoName).futureValue shouldBe false
    }

    "throw exception when github returns anything other than 200 or 404" in {

      givenGitHubExpects(
        method          = GET,
        url             = urls.containsRepo(repoName),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (999, None)
      )

      intercept[RequestException] {
        Await.result(github.containsRepo(repoName), 5 seconds)
      }
    }
  }

  "Github.teamId" should {
    "find a team ID for a team name when the team exists" in {
      givenGitHubExpects(
        method       = GET,
        url          = urls.teams(),
        extraHeaders = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (
          200,
          Some("""
            |[
            |  {
            |    "id": 1,
            |    "url": "https://api.github.com/teams/1",
            |    "name": "Justice League"
            |  },
            |  {
            |    "id": 2,
            |    "url": "https://api.github.com/teams/1",
            |    "name": "Auth"
            |  }
            |]
          """.stripMargin))
      )

      printMappings()
      github.teamId("Auth").futureValue.get shouldBe 2
    }

    "support pagination" in {

      val page1 = 1 to 100 map { id =>
        Team(id, s"https://api.github.com/teams/$id", s"Team $id")
      }
      val page2 = 101 to 200 map { id =>
        Team(id, s"https://api.github.com/teams/$id", s"Team $id")
      }
      val page3 = 201 to 250 map { id =>
        Team(id, s"https://api.github.com/teams/$id", s"Team $id")
      }

      givenGitHubExpects(
        method          = GET,
        url             = urls.teams(),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (200, Some(Json.toJson(page1).toString))
      )

      givenGitHubExpects(
        method          = GET,
        url             = new URL(urls.teams().toString + "&page=2"),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (200, Some(Json.toJson(page2).toString))
      )

      givenGitHubExpects(
        method          = GET,
        url             = new URL(urls.teams().toString + "&page=3"),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (200, Some(Json.toJson(page3).toString))
      )

      printMappings()
      1 to 250 foreach { id =>
        github.teamId(s"Team $id").futureValue shouldBe Some(id)
      }
    }

    "return None when the team does not exist" in {
      givenGitHubExpects(
        method       = GET,
        url          = urls.teams(),
        extraHeaders = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (
          200,
          Some("""
            |[
            |  {
            |    "id": 1,
            |    "url": "https://api.github.com/teams/1",
            |    "name": "Justice League"
            |  },
            |  {
            |    "id": 2,
            |    "url": "https://api.github.com/teams/2",
            |    "name": "Auth"
            |  }
            |]
          """.stripMargin))
      )

      printMappings()
      github.teamId("MDTP").futureValue shouldBe None
    }
  }

  "Github.createRepo" should {

    "successfully create a public repo" in {

      givenGitHubExpects(
        method          = POST,
        url             = urls.createRepo,
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (201, None)
      )

      val createdUrl = github.createRepo(repoName, privateRepo = false).futureValue
      createdUrl shouldBe s"https://github.com/hmrc/$repoName"

      assertRequest(
        method = POST,
        url    = urls.createRepo,
        body   = Some(s"""{
             |    "name": "$repoName",
             |    "description": "",
             |    "homepage": "",
             |    "private": false,
             |    "has_issues": true,
             |    "has_wiki": true,
             |    "license_template": "apache-2.0",
             |    "has_downloads": true
             |}""".stripMargin)
      )
    }
    "successfully create a private repo" in {

      givenGitHubExpects(
        method          = POST,
        url             = urls.createRepo,
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (201, None)
      )

      val createdUrl = github.createRepo(repoName, privateRepo = true).futureValue

      createdUrl shouldBe s"https://github.com/hmrc/$repoName"

      assertRequest(
        method = POST,
        url    = urls.createRepo,
        body   = Some(s"""{
             |    "name": "$repoName",
             |    "description": "",
             |    "homepage": "",
             |    "private": true,
             |    "has_issues": true,
             |    "has_wiki": true,
             |    "has_downloads": true
             |}""".stripMargin)
      )
    }
  }

  "Github.addRepoToTeam" should {
    "add a repository to a team in" in {
      givenGitHubExpects(
        method          = PUT,
        url             = urls.addTeamToRepo(repoName, 99),
        extraHeaders    = Map("Authorization" -> basicAuthHeader),
        willRespondWith = (204, None)
      )

      printMappings()
      val future = github.addRepoToTeam(repoName, 99, "push")
      Await.result(future, 5.seconds)
      future.value.get.isSuccess shouldBe true

      assertRequest(
        method       = PUT,
        url          = urls.addTeamToRepo(repoName, 99),
        extraHeaders = Map("Accept" -> "application/vnd.github.ironman-preview+json"),
        body         = Some("""{"permission": "push"}""")
      )
    }
  }

  "Github addRequireSignedCommits" should {
    "enforce signed commits required on the relevant branch" in {
      givenGitHubExpects(
        method          = PUT,
        url             = urls.addBranchProtection(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.luke-cage-preview+json"),
        willRespondWith = (200, None)
      )

      givenGitHubExpects(
        method          = POST,
        url             = urls.addRequireSignedCommits(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.zzzax-preview+json"),
        willRespondWith = (200, None)
      )

      val futureResponse = github.addRequireSignedCommits(repoName, Seq("master"))
      val response = Await.result(futureResponse, 5.seconds)
      response shouldBe s"Enabled require signed commits for repo $repoName on branch master"
    }

    "return a helpful error if the require signed commits call fails" in {
      givenGitHubExpects(
        method          = PUT,
        url             = urls.addBranchProtection(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.luke-cage-preview+json"),
        willRespondWith = (200, None)
      )

      givenGitHubExpects(
        method          = POST,
        url             = urls.addRequireSignedCommits(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.zzzax-preview+json"),
        willRespondWith = (500, Some("This didn't work"))
      )

      val futureResponse = github.addRequireSignedCommits(repoName, Seq("master"))
      val error = intercept[Exception] {
        Await.result(futureResponse, 5.seconds)
      }

      error.getMessage should include("Didn't get expected status code when writing to")
      error.getMessage should include("/repos/hmrc/domain/branches/master/protection/required_signatures")
      error.getMessage should include("This didn't work")
    }

    "return a helpful error if the enabling branch protection fails" in {
      givenGitHubExpects(
        method          = PUT,
        url             = urls.addBranchProtection(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.luke-cage-preview+json"),
        willRespondWith = (400, Some("Branch protection didn't work"))
      )

      givenGitHubExpects(
        method          = POST,
        url             = urls.addRequireSignedCommits(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.zzzax-preview+json"),
        willRespondWith = (500, Some("Signed commits didn't work"))
      )

      val futureResponse = github.addRequireSignedCommits(repoName, Seq("master"))
      val error = intercept[Exception] {
        Await.result(futureResponse, 5.seconds)
      }

      error.getMessage should include("Didn't get expected status code when writing to")
      error.getMessage should include("/repos/hmrc/domain/branches/master/protection")
      error.getMessage should include("Branch protection didn't work")
    }

    "return a helpful error if some calls fail and some succeed" in {
      givenGitHubExpects(
        method          = POST,
        url             = urls.addRequireSignedCommits(repoName, "SOME-123"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.zzzax-preview+json"),
        willRespondWith = (200, None)
      )

      givenGitHubExpects(
        method          = POST,
        url             = urls.addRequireSignedCommits(repoName, "master"),
        extraHeaders    = Map(
          "Authorization" -> basicAuthHeader,
          "Accept" -> "application/vnd.github.zzzax-preview+json"),
        willRespondWith = (500, Some("This didn't work"))
      )

      val futureResponse = github.addRequireSignedCommits(repoName, Seq("SOME-123", "master"))
      val error = intercept[Exception] {
        Await.result(futureResponse, 5.seconds)
      }

      println(error.getMessage)
      error.getMessage should fullyMatch
        "Didn't get expected status code when writing to [*]/repos/hmrc/domain/branches/master/protection/required_signatures. " +
          "Got status 500: POST [*]/repos/hmrc/domain/branches/master/protection/required_signatures This didn't work".r
    }
  }

    case class Team(id: Int, url: String, name: String)
  implicit val f = Json.format[Team]

  case class GithubRequest(method: RequestMethod, url: String, body: Option[String]) {
    def req: RequestPatternBuilder = {
      val builder = new RequestPatternBuilder(method, urlEqualTo(url))
      body
        .map { b =>
          builder.withRequestBody(equalToJson(b))
        }
        .getOrElse(builder)
    }
  }

  def assertRequest(
    method: RequestMethod,
    url: URL,
    extraHeaders: Map[String, String] = Map(),
    body: Option[String]): Unit = {

    val builder = new RequestPatternBuilder(method, urlPathEqualTo(url.getPath))
    extraHeaders.foreach {
      case (k, v) =>
        builder.withHeader(k, equalTo(v))
    }

    body
      .map { b =>
        builder.withRequestBody(equalToJson(b))
      }
      .getOrElse(builder)

    endpointMock.verifyThat(builder)
  }

  def assertRequest(req: GithubRequest): Unit =
    endpointMock.verifyThat(req.req)
}
