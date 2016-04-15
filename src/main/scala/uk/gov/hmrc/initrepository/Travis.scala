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

import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TravisConnector {

  def httpTransport: HttpTransport

  def travisUrls : TravisUrls

  def authenticate: Future[TravisAuthenticationResult] = {
    val req = httpTransport.buildJsonCall(
        "Get",
        travisUrls.githubAuthentication,
        Some(Json.obj("github_token" -> httpTransport.creds.pass)))
      .withHeaders(standardHeaders: _*)

    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(new TravisAuthenticationResult((res.json \ "access_token").as[String]))
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def syncWithGithub: Future[Unit] = {
    val req = post(travisUrls.syncWithGithub)
    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(Unit)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  private def post(url: URL) =
    httpTransport.buildJsonCall("POST", url).withHeaders(standardHeaders: _*)

  private val standardHeaders = Seq(
    "User-Agent" -> "Travis/1.0",
    "Accept" -> "application/vnd.travis-ci.2+json")

}

class TravisUrls(apiRoot:String = "https://api.github.com"){
  def githubAuthentication: URL = new URL(s"$apiRoot/auth/github")
  def syncWithGithub: URL = new URL(s"$apiRoot/users/sync")
}

case class TravisAuthenticationResult(accessToken: String)
