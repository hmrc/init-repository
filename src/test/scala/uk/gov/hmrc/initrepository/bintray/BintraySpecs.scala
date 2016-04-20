/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.initrepository.bintray

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, RequestPatternBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.initrepository._
import uk.gov.hmrc.initrepository.wiremock.WireMockEndpoints


class BintraySpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints {

  val transport = new HttpTransport {
    override def creds: ServiceCredentials = ServiceCredentials("", "")
  }

  val bintrayUrls = new BintrayUrls(apiRoot = endpointMockUrl)
  val bintray = new Bintray{
    override def http: HttpTransport = transport
    override def urls: BintrayUrls = bintrayUrls
  }

  "Bintray.containsPackage" should {

    "return true when bintray returns 200" in {

      expectHttp(
        method = GET,
        url = bintrayUrls.containsPackage("releases", "domain"),
        extraHeaders = Map("Authorization" -> transport.creds.toBasicAuth),
        willRespondWith = (200, None)
      )

      bintray.containsPackage("releases", "domain").await shouldBe true

    }

    "return false when bintray returns 404" in {

      expectHttp(
        method = GET,
        url = bintrayUrls.containsPackage("releases", "domain"),
        extraHeaders = Map("Authorization" -> transport.creds.toBasicAuth),
        willRespondWith = (404, None)
      )

      bintray.containsPackage("releases", "domain").await shouldBe false
    }

    "return false when bintray returns anything other than 200 or 404" in {

      expectHttp(
        method = GET,
        url = bintrayUrls.containsPackage("releases", "domain"),
        extraHeaders = Map("Authorization" -> transport.creds.toBasicAuth),
        willRespondWith = (999, None)
      )

      intercept[RequestException]{
        bintray.containsPackage("releases", "domain").await
      }
    }
  }

  "Bintray.createPackage" should {

    "create a package" in {

      expectHttp(
        method = POST,
        url = bintrayUrls.createPackage("releases"),
        extraHeaders = Map("Authorization" -> transport.creds.toBasicAuth),
        willRespondWith = (201, None)
      )

      printMappings()
      bintray.createPackage("releases", "domain").awaitSuccess()

      assertRequest(
        method = POST,
        url = bintrayUrls.createPackage("releases"),
        body = Some(
          """ {
                      |    "name": "domain",
                      |    "desc": "domain releases",
                      |    "labels": [],
                      |    "licenses": ["Apache-2.0"],
                      |    "vcs_url": "https://github.com/hmrc/domain",
                      |    "website_url": "https://github.com/hmrc/domain",
                      |    "issue_tracker_url": "https://github.com/hmrc/domain/issues",
                      |    "github_repo": "hmrc/domain",
                      |    "public_download_numbers": true,
                      |    "public_stats": true
                      |}""".stripMargin)
      )
    }

    "return future.failed when repo isn't created" in {
      expectHttp(
        method = POST,
        url = bintrayUrls.createPackage("releases"),
        extraHeaders = Map("Authorization" -> transport.creds.toBasicAuth),
        willRespondWith = (999, None)
      )

      intercept[RequestException]{
        bintray.createPackage("releases", "domain").awaitFailure()
      }
    }
  }

  "BintrayConfig" should {
    "return correct repository names based on repository type" in {
      BintrayConfig.apply(RepositoryType.SbtPlugin).head should startWith("sbt")
      BintrayConfig.apply(RepositoryType.Sbt).head should not startWith "sbt"
    }

    "return the releases repository name" in {
      BintrayConfig.releasesRepositoryNameFor(RepositoryType.SbtPlugin) shouldBe "sbt-plugin-releases"
      BintrayConfig.releasesRepositoryNameFor(RepositoryType.Sbt) shouldBe "releases"
    }
  }

  def assertRequest(method:RequestMethod, url:URL, body:Option[String]): Unit ={
    val builder = new RequestPatternBuilder(method, urlPathEqualTo(url.getPath))
    body.map{ b =>
      builder.withRequestBody(equalToJson(b))
    }.getOrElse(builder)
    endpointMock.verifyThat(builder)
  }

}
