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

package uk.gov.hmrc.initrepository

import java.net.URL

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient, NingWSClientConfig}
import play.api.libs.ws.{WSAuthScheme, WSRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait HttpTransport {

  def creds:ServiceCredentials

  private val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

  def close() = {
    ws.close()
    Log.debug("closing http client")
  }

  def buildJsonCall(method:String, url:URL, body:Option[JsValue] = None):WSRequest={
    val req = ws.url(url.toString)
      .withMethod(method)
      .withAuth(creds.user, creds.pass, WSAuthScheme.BASIC)
      .withHeaders(
        "content-type" -> "application/json")

    Log.debug("req = " + req)

    body.map { b =>
      req.withBody(b)
    }.getOrElse(req)
  }

  def postJsonString(url:URL, body:String): Future[String] = {
    buildJsonCall("POST", url, Some(Json.parse(body))).execute().flatMap { case result =>
      result.status match {
        case s if s >= 200 && s < 300 => Future.successful(result.body)
        case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when writing to ${url}. Got status ${result.status}: POST ${url} ${result.body}"))
      }
    }
  }

}
