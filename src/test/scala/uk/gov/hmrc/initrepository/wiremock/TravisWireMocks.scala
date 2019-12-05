/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.initrepository.wiremock

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod

trait TravisWireMocks {
  this: WireMockEndpoints =>

  def givenTravisExpects(
    method: RequestMethod,
    url: URL,
    payload: Option[String]           = None,
    extraHeaders: Map[String, String] = Map(),
    willRespondWith: (Int, Option[String])) =
    expectHttp(
      method,
      url,
      payload,
      extraHeaders ++ Map("User-Agent" -> "Travis/1.0", "Accept" -> "application/vnd.travis-ci.2+json"),
      willRespondWith)

  def verifyNoAuthHeader(url: URL) =
    endpointMock.verifyThat(postRequestedFor(urlPathEqualTo(url.getPath)).withoutHeader("Authorization"))
}
