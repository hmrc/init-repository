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

import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.RequestMethod

trait GithubWireMocks {
  this: WireMockEndpoints =>

  def givenGitHubExpects(
                          method:RequestMethod,
                          url:String,
                          extraHeaders:Map[String,String] = Map(),
                          willRespondWith: (Int, Option[String])): Unit = {

    val builder = new MappingBuilder(method, urlEqualTo(url))
    
    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val resp = willRespondWith._2.map { b =>
      response.withBody(b)
    }.getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }
}
