/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient, NingWSClientConfig}
import play.api.libs.ws.{WSAuthScheme, WSRequest}

trait HttpTransport {

  def creds:ServiceCredentials

  private val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

  def close() = {
    ws.close()
    Log.debug("closing http client")
  }

  private def expandQueryParam(param: String) = {
    val pair = param.split("=")
    pair.head -> pair.last
  }

  private def applyBody(body:Option[JsValue])(req: WSRequest): WSRequest =
    body.map { b => req.withBody(b) }.getOrElse(req)

  private def applyQueryParams(url: URL)(req: WSRequest): WSRequest = {
    Option(url.getQuery) match {
      case Some(query: String) => req.withQueryString(query.split("&") map expandQueryParam: _*)
      case _ => req
    }
  }

  def buildJsonCall(method:String, url:URL, body:Option[JsValue] = None):WSRequest={
    val urlWithoutQuery = url.toString.split('?').head
    val req = ws.url(urlWithoutQuery)
      .withMethod(method)
      .withFollowRedirects(false)

    Function.chain(Seq(
      applyBody(body) _,
      applyQueryParams(url) _
    ))(req)
  }

  def buildJsonCallWithAuth(method:String, url:URL, body:Option[JsValue] = None): WSRequest =
    buildJsonCall(method, url, body).withAuth(creds.user, creds.pass, WSAuthScheme.BASIC)

}
