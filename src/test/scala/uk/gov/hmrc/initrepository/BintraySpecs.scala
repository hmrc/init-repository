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

import com.github.tomakehurst.wiremock.client.{ResponseDefinitionBuilder, MappingBuilder, RequestPatternBuilder}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalatest.{WordSpec, Matchers, WordSpecLike}


class BintraySpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints {

  class FakeBintrayHttp extends BintrayHttp {
    override def creds: ServiceCredentials = ServiceCredentials("", "")
  }

  val bintray = new Bintray(new FakeBintrayHttp, new BintrayUrls(apiRoot = endpointMockUrl))
  
  "Bintray.containsPackage" should {

    "return true when bintray returns 200" in {

      givenServerExpects(
        method = GET,
        url = "/packages/hmrc/releases/domain",
        willRespondWith = (200, None)
      )

      bintray.containsPackage("releases", "domain").await// shouldBe true

    }

    "return false when bintray returns 404" in {

      givenServerExpects(
        method = GET,
        url = "/packages/hmrc/releases/domain",
        willRespondWith = (404, None)
      )

      bintray.containsPackage("releases", "domain").await shouldBe false
    }

    "return false when bintray returns anything other than 200 or 404" in {

      givenServerExpects(
        method = GET,
        url = "/packages/hmrc/releases/domain",
        willRespondWith = (999, None)
      )

      bintray.containsPackage("releases", "domain").await shouldBe false
    }
  }

  def assertRequest(method:RequestMethod, url:String, body:Option[String]): Unit ={
    val builder = new RequestPatternBuilder(method, urlEqualTo(url))
    body.map{ b =>
      builder.withRequestBody(equalToJson(b))
    }.getOrElse(builder)
    endpointMock.verifyThat(builder)
  }


  def givenServerExpects(method:RequestMethod, url:String, willRespondWith: (Int, Option[String])): Unit = {

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
