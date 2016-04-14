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
import java.nio.file.{Paths, Files}

import uk.gov.hmrc.initrepository.git.Command
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient, NingWSClientConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GithubUrls( orgName:String = "hmrc",
                  apiRoot:String = "https://api.github.com"){

  def createRepo: URL =
    new URL(s"$apiRoot/orgs/$orgName/repos")

  def containsRepo(repo:String) =
    new URL(s"$apiRoot/repos/$orgName/$repo")

  def teams =
    new URL(s"$apiRoot/orgs/$orgName/teams?per_page=100")

  def addTeamToRepo(repoName:String, teamId:Int) =
    new URL(s"$apiRoot/teams/$teamId/repos/$orgName/$repoName?permission=push")
}

class RequestException(request:WSRequest, response:WSResponse)
  extends Exception(s"Got status ${response.status}: ${request.method} ${request.url} ${response.body}"){

}

trait Github{

  def githubHttp:GithubHttp

  def githubUrls:GithubUrls

  val IronManApplication = "application/vnd.github.ironman-preview+json"

  def teamId(team: String): Future[Option[Int]]={
    val req = githubHttp.buildJsonCall("GET", githubUrls.teams)


    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(findIdForName(res.json, team))
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def addRepoToTeam(repoName: String, teamId: Int):Future[Unit] = {
    Log.info(s"Adding $repoName to team ${teamId}")

    val req = githubHttp
      .buildJsonCall("PUT", githubUrls.addTeamToRepo(repoName, teamId))
      .withHeaders("Accept" -> IronManApplication)
      .withHeaders("Content-Length" -> "0")
      .withBody("""{"permission": "push"}"""")


    req.execute().flatMap { res => res.status match {
      case 204 => Future.successful(Unit)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def findIdForName(json:JsValue, teamName:String):Option[Int]={
    json.as[JsArray].value
      .find(j => (j \ "name").toOption.exists(s => s.as[JsString].value == teamName))
      .map(j => (j \ "id").get.as[JsNumber].value.toInt)
  }


  def containsRepo(repoName: String): Future[Boolean] = {
    val req = githubHttp.buildJsonCall("GET", githubUrls.containsRepo(repoName))

    req.execute().flatMap { res => res.status match {
      case 200 => Future.successful(true)
      case 404 => Future.successful(false)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def createRepo(repoName: String): Future[String] = {
    Log.info(s"creating github repository with name '${repoName}'")
    val payload = s"""{
                    |    "name": "$repoName",
                    |    "description": "",
                    |    "homepage": "",
                    |    "private": false,
                    |    "has_issues": true,
                    |    "has_wiki": true,
                    |    "has_downloads": true,
                    |    "license_template": "apache-2.0"
                    |}""".stripMargin

      githubHttp.postJsonString(githubUrls.createRepo, payload).map { _ => s"git@github.com:hmrc/$repoName.git" }
  }

  def close() = githubHttp.close()
}

case class SimpleResponse(status:Int, rawBody:String)


trait GithubHttp {

  def creds:ServiceCredentials

  private val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

  def close() = {
    ws.close()
    Log.debug("closing github http client")
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

  def getBody(url:URL): Future[String] = {
    get(url).map(_.body)
  }

  def get(url:URL): Future[WSResponse] = {
    val resultF = buildJsonCall("GET", url).execute()
    resultF.flatMap { res => res.status match {
      case s if s >= 200 && s < 300 => Future.successful(res)
      case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when reading from Github. Got status ${res.status}: GET ${url} ${res.body}"))
    }}
  }

  def postJsonString(url:URL, body:String): Future[String] = {
    buildJsonCall("POST", url, Some(Json.parse(body))).execute().flatMap { case result =>
      result.status match {
        case s if s >= 200 && s < 300 => Future.successful(result.body)
        case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when writing to Github. Got status ${result.status}: POST ${url} ${result.body}"))
      }
    }
  }
}
