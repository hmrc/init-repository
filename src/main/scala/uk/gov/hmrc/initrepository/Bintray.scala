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

import java.net.URL

import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class BintrayUrls(apiRoot:String = "https://bintray.com/api/v1"){
  def containsPackage(repoName: String, packageName: String):URL =
    new URL(s"$apiRoot/packages/hmrc/$repoName/$packageName")
}

class Bintray(http:BintrayHttp, urls:BintrayUrls){

  def createRepo(repoName: String) = ???

  val log = new Logger()

  def containsPackage(repoName: String, packageName:String): Future[Boolean] = {
    val req = http.buildJsonCall("GET", urls.containsPackage(repoName, packageName))

    req.execute() flatMap { res => res.status match {
      case 200 => Future.successful(true)
      case 404 => Future.successful(false)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }
}


trait BintrayHttp{

  def creds:ServiceCredentials

  val log = new Logger()

  val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new WSClientConfig()).build())

  def buildJsonCall(method:String, url:URL, body:Option[JsValue] = None):WSRequestHolder= {
    log.debug(s"bintray client_id ${creds.user.takeRight(5)}")
    log.debug(s"bintray client_secret ${creds.pass.takeRight(5)}")

    val req = ws.url(url.toString)
      .withMethod(method)
      .withAuth(creds.user, creds.pass, WSAuthScheme.BASIC)
      .withQueryString(
        "client_id" -> creds.user,
        "client_secret" -> creds.pass)
      .withHeaders(
        "content-type" -> "application/json")

    println("req = " + req)

    body.map { b =>
      req.withBody(b)
    }.getOrElse(req)
  }
}
