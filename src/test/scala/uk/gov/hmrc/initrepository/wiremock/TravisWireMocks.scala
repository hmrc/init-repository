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

package uk.gov.hmrc.initrepository.wiremock

import java.net.URL

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.http.RequestMethod

trait TravisWireMocks {
  this: WireMockEndpoints =>

  def givenTravisExpects(
    method:RequestMethod,
    url:URL,
    payload: Option[String] = None,
    extraHeaders:Map[String,String] = Map(),
    willRespondWith: (Int, Option[String])): Unit = {

    val builder = payload.map {
      json => createBuilder(method, url, extraHeaders)
        .withHeader("Content-Type", equalTo("application/json; charset=utf-8"))
        .withRequestBody(equalToJson(json))
    }.getOrElse(createBuilder(method, url, extraHeaders))

    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val resp = willRespondWith._2.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }

  private def applyExtraHeaders(extraHeaders: Map[String, String])(builder: MappingBuilder) =
    extraHeaders.foldLeft(builder) { (builder, header) =>
      builder.withHeader(header._1, equalTo(header._2)) }

  private def applyQueryString(url: URL)(builder: MappingBuilder) =
    Option(url.getQuery) match {
      case Some(query: String) => query.split("&").map { q => q.split("=") }
        .foldLeft(builder) { (builder, queryParam) =>
          builder.withQueryParam(queryParam.head, equalTo(queryParam.last)) }
      case _ => builder
    }

  private def createBuilder(method: RequestMethod, url: URL, extraHeaders: Map[String, String]): MappingBuilder = {
    val builder = new MappingBuilder(method, urlPathEqualTo(url.getPath))
      .withHeader("User-Agent", equalTo("Travis/1.0"))
      .withHeader("Accept", equalTo("application/vnd.travis-ci.2+json"))

    Function.chain(Seq(
      applyExtraHeaders(extraHeaders) _,
      applyQueryString(url) _))(builder)
  }
}
