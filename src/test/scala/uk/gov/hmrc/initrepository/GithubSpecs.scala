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

package uk.gov.hmrc.initrepository

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, RequestPatternBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json



class GithubSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints {

  class FakeGithubHttp extends GithubHttp {
    override def creds: ServiceCredentials = ServiceCredentials("", "")
  }

  val githubUrls = new GithubUrls(apiRoot = endpointMockUrl)
  val github: Github = new Github(new FakeGithubHttp(), githubUrls)

  "Github.containsRepo" should {

    "return true when github returns 200" in {

      givenGitHubExpects(
        method = GET,
        url = "/repos/hmrc/domain?client_id=&client_secret=",
        willRespondWith = (200, None)
      )

      github.containsRepo("domain").await shouldBe true
    }

    "return false when github returns 404" in {

      givenGitHubExpects(
        method = GET,
        url = "/repos/hmrc/domain?client_id=&client_secret=",
        willRespondWith = (404, None)
      )

      github.containsRepo("domain").await shouldBe false
    }

    "throw exception when github returns anything other than 200 or 404" in {

      givenGitHubExpects(
        method = GET,
        url = "/repos/hmrc/domain?client_id=&client_secret=",
        willRespondWith = (999, None)
      )

      intercept[RequestException]{
        github.containsRepo("domain").await
      }
    }
  }

  "Github.createRepo" should {

    "successfully create repo" in {

      givenGitHubExpects(
        method = POST,
        url = "/orgs/hmrc/repos?client_id=&client_secret=",
        willRespondWith = (201, None)
      )

      github.createRepo("domain").awaitSuccessOrThrow

      assertRequest(
        method = POST,
        url = "/orgs/hmrc/repos?client_id=&client_secret=",
        body = Some("""{
                      |    "name": "domain",
                      |    "description": "domain",
                      |    "homepage": "https://github.com",
                      |    "private": true,
                      |    "has_issues": true,
                      |    "has_wiki": true,
                      |    "has_downloads": true,
                      |    "license_template": "apache-2.0"
                      |}""".stripMargin)
      )
    }
  }

  case class GithubRequest(method:RequestMethod, url:String, body:Option[String]){

    {
      body.foreach { b => Json.parse(b) }
    }

    def req:RequestPatternBuilder = {
      val builder = new RequestPatternBuilder(method, urlEqualTo(url))
      body.map{ b =>
        builder.withRequestBody(equalToJson(b))
      }.getOrElse(builder)
    }
  }

  def assertRequest(method:RequestMethod, url:String, body:Option[String]): Unit ={
    val builder = new RequestPatternBuilder(method, urlEqualTo(url))
    body.map{ b =>
      builder.withRequestBody(equalToJson(b))
    }.getOrElse(builder)
    endpointMock.verifyThat(builder)
  }

  def assertRequest(req:GithubRequest): Unit ={
    endpointMock.verifyThat(req.req)
  }

  def givenGitHubExpects(method:RequestMethod, url:String, willRespondWith: (Int, Option[String])): Unit = {

    val builder = new MappingBuilder(method, urlEqualTo(url))
      .withHeader("Content-Type", equalTo("application/json"))

    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val resp = willRespondWith._2.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    willRespondWith._2.map { b =>
      builder.withRequestBody(equalToJson(b))
    }.getOrElse(builder)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }
}
