/*
 * Copyright 2017 HM Revenue & Customs
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
import java.util.{Timer, TimerTask}

import play.api.libs.json.{Format, JsValue, Json}
import play.api.libs.ws.WSResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

trait TravisConnector {

  def httpTransport: HttpTransport

  def travisUrls : TravisUrls

  def authenticate: Future[TravisAuthenticationResult] = {
    Log.info(s"Fetching travis access token")

    val req = post(
      travisUrls.githubAuthentication,
      Some(Json.obj("github_token" -> httpTransport.creds.pass)))

    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(new TravisAuthenticationResult((res.json \ "access_token").as[String]))
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def syncWithGithub(accessToken: String): Future[Unit] = {
    Log.info(s"Synchronising with github")

    val req = post(travisUrls.syncWithGithub)
      .withHeaders("Authorization" -> s"token $accessToken")

    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(Unit)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def searchForRepo(accessToken: String, repositoryName: String)(implicit backoffStrategy: TravisSearchBackoffStrategy) : Future[Int] = {
    import FutureUtils._

    Log.info(s"Attempting to locate $repositoryName in travis")

    val req = get(travisUrls.searchForRepo(repositoryName))
      .withHeaders("Authorization" -> s"token $accessToken")

    exponentialRetry(backoffStrategy.retries, backoffStrategy.initialDuration) {
      req.execute().flatMap { res =>
        res.status match {
          case 200 =>
            extractSearchResults(res, repositoryName) match {
              case Some(r) => Future.successful(r.id)
              case _ => Future.failed(new TravisSearchException(repositoryName))
            }
          case _ => Future.failed(new RequestException(req, res))
        }
      }
    }
  }

  def activateHook(accessToken: String, repositoryId: Int): Future[Unit] = {
    Log.info(s"Activating travis hook")

    val req = put(
      travisUrls.activateHook,
      Some(Json.obj("hook" -> Json.obj("id" -> repositoryId, "active" -> true))))
      .withHeaders("Authorization" -> s"token $accessToken")

    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(Unit)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def close() = httpTransport.close()

  private def extractSearchResults(res: WSResponse, repositoryName: String) : Option[SearchForRepositoryResult] = {
    import SearchForRepositoryResult._

    (res.json \ "repos").asOpt[Seq[SearchForRepositoryResult]] match {
      case Some(results) =>
        results.find(r => r.slug == s"hmrc/$repositoryName")
      case _ =>
        Log.debug("Could not parse json response from travis repository search")
        None
    }
  }

  private def get(url: URL, body:Option[JsValue] = None) =
   httpTransport.buildJsonCall("GET", url, body).withHeaders(standardHeaders: _*)

  private def post(url: URL, body:Option[JsValue] = None) =
    httpTransport.buildJsonCall("POST", url, body).withHeaders(standardHeaders: _*)

  private def put(url: URL, body:Option[JsValue] = None) =
    httpTransport.buildJsonCall("PUT", url, body).withHeaders(standardHeaders: _*)

  private val standardHeaders = Seq(
    "User-Agent" -> "Travis/1.0",
    "Accept" -> "application/vnd.travis-ci.2+json")

}

class TravisUrls(apiRoot:String = "https://api.travis-ci.org"){
  def githubAuthentication: URL = new URL(s"$apiRoot/auth/github")
  def syncWithGithub: URL = new URL(s"$apiRoot/users/sync")
  def searchForRepo(newRepoName: String) = new URL(s"$apiRoot/repos/hmrc?search=$newRepoName")
  def activateHook = new URL(s"$apiRoot/hooks")
}

case class TravisAuthenticationResult(accessToken: String)

case class SearchForRepositoryResult(id: Int, slug: String)

case class TravisSearchBackoffStrategy(retries: Int = 10, initialDuration: Double = 150)

object SearchForRepositoryResult {
  implicit val jsonFormat: Format[SearchForRepositoryResult] = Json.format[SearchForRepositoryResult]
}
