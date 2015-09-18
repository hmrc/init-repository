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

  "Github.containsRepo" should {

    val github: Github = new Github(new FakeGithubHttp(), githubUrls)

    "return true when github returns 200" in {

      givenGitHubExpects(
        method = HEAD,
        url = "/hmrc/domain",
        willRespondWith = (200, None)
      )

      github.containsRepo("domain").await shouldBe true
    }

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


//    def givenGitHubExpects(req:GithubRequest, willRespondWith: (Int, Option[String])): Unit = {
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
