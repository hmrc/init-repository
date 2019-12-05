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

import java.net.{ServerSocket, URL}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.http.RequestMethod
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.collection.JavaConversions._
import scala.util.Try

trait WireMockEndpoints extends Suite with BeforeAndAfterAll with BeforeAndAfterEach {

  val host: String = "localhost"

  val endpointPort: Int              = PortTester.findPort()
  val endpointMock                   = new WireMock(host, endpointPort)
  val endpointMockUrl                = s"http://$host:$endpointPort"
  val endpointServer: WireMockServer = new WireMockServer(wireMockConfig().port(endpointPort))

  def startWireMock() = endpointServer.start()
  def stopWireMock()  = endpointServer.stop()

  override def beforeEach(): Unit = {
    endpointMock.resetMappings()
    endpointMock.resetScenarios()
  }
  override def afterAll(): Unit =
    endpointServer.stop()
  override def beforeAll(): Unit =
    endpointServer.start()

  def printMappings(): Unit =
    endpointMock.allStubMappings().getMappings.toList.foreach { s =>
      println(s)
    }

  protected def expectHttp(
    method: RequestMethod,
    url: URL,
    payload: Option[String]           = None,
    extraHeaders: Map[String, String] = Map(),
    willRespondWith: (Int, Option[String])) = {

    val builder = payload
      .map { json =>
        createBuilder(method, url, extraHeaders)
          .withHeader("Content-Type", equalTo("application/json; charset=utf-8"))
          .withRequestBody(equalToJson(json))
      }
      .getOrElse(createBuilder(method, url, extraHeaders))

    val response: ResponseDefinitionBuilder = new ResponseDefinitionBuilder()
      .withStatus(willRespondWith._1)

    val resp = willRespondWith._2
      .map { b =>
        response.withBody(b)
      }
      .getOrElse(response)

    builder.willReturn(resp)

    endpointMock.register(builder)
  }

  private def createBuilder(method: RequestMethod, url: URL, extraHeaders: Map[String, String]): MappingBuilder = {
    val builder = new MappingBuilder(method, urlPathEqualTo(url.getPath))

    Function.chain(Seq(applyExtraHeaders(extraHeaders) _, applyQueryString(url) _))(builder)
  }

  protected def applyExtraHeaders(extraHeaders: Map[String, String])(builder: MappingBuilder) =
    extraHeaders.foldLeft(builder) { (builder, header) =>
      builder.withHeader(header._1, equalTo(header._2))
    }

  protected def applyQueryString(url: URL)(builder: MappingBuilder) =
    Option(url.getQuery) match {
      case Some(query: String) =>
        query
          .split("&")
          .map { q =>
            q.split("=")
          }
          .foldLeft(builder) { (builder, queryParam) =>
            builder.withQueryParam(queryParam.head, equalTo(queryParam.last))
          }
      case _ => builder
    }

}

object PortTester {

  def findPort(excluded: Int*): Int =
    (6001 to 7000).find(port => !excluded.contains(port) && isFree(port)).getOrElse(throw new Exception("No free port"))

  private def isFree(port: Int): Boolean = {
    val triedSocket = Try {
      val serverSocket = new ServerSocket(port)
      Try(serverSocket.close())
      serverSocket
    }
    triedSocket.isSuccess
  }
}
