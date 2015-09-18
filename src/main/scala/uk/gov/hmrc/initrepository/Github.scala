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
import java.util.concurrent.TimeUnit

import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingWSClientConfig, NingAsyncHttpClientConfigBuilder, NingWSClient}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class GithubUrls(webRoot:String = "https://github.com/hmrc", apiRoot:String = "https://api.github.com"){
  def createRepo: URL = new URL(s"$apiRoot/orgs/hmrc/repos")

  def containsRepoUrl(repo:String):URL={
    new URL(s"$webRoot/$repo")
  }
}

class Github(githubHttp:GithubHttp, githubUrls:GithubUrls){

  val orgName = "hmrc"

  def containsRepo(repoName: String): Future[Boolean] = {
    githubHttp.head(githubUrls.containsRepoUrl(repoName)).map(r => {println(r);r == 200})
  }

  def createRepo(repoName: String): Future[Unit] = {
    val payload = s"""{
                    |    "name": "$repoName",
                    |    "description": "$repoName",
                    |    "homepage": "https://github.com",
                    |    "private": true,
                    |    "has_issues": true,
                    |    "has_wiki": true,
                    |    "has_downloads": true,
                    |    "license_template": "apache-2.0"
                    |}""".stripMargin

      githubHttp.postJsonString(githubUrls.createRepo, payload).map { _ => Unit }
  }
}

case class SimpleResponse(status:Int, rawBody:String)


trait GithubHttp{

  def creds:ServiceCredentials

  val log = new Logger()

  val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build())

  def buildJsonCall(method:String, url:URL, body:Option[JsValue] = None):WSRequestHolder={
    log.debug(s"github client_id ${creds.user.takeRight(5)}")
    log.debug(s"github client_secret ${creds.pass.takeRight(5)}")

    val req = ws.url(url.toString)
      .withMethod(method)
      .withAuth(creds.user, creds.pass, WSAuthScheme.BASIC)
      .withQueryString(
        "client_id" -> creds.user,
        "client_secret" -> creds.pass)
      .withHeaders(
        "content-type" -> "application/json")

    body.map { b =>
      req.withBody(b)
    }.getOrElse(req)
  }

  def head(url:URL):Future[Int]={
    buildJsonCall("HEAD", url).execute().map(_.status)
  }

  def callAndWait(req:WSRequest): WSResponse = {

    log.info(s"${req.method} with ${req.url}")

    val result: WSResponse = Await.result(req.execute(), Duration.apply(1, TimeUnit.MINUTES))

    log.info(s"${req.method} with ${req.url} result ${result.status} - ${result.statusText}")

    result
  }

  def get(url:URL): Try[Unit] = {
    val result = callAndWait(buildJsonCall("GET", url))
    result.status match {
      case s if s >= 200 && s < 300 => Success(Unit)
      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when writing to Github. Got status ${result.status}: ${result.body}"))
    }
  }

  def post[A](responseBuilder:(WSResponse) => Try[A])(url:URL, body:JsValue): Try[A] = {
    val result = callAndWait(buildJsonCall("POST", url, Some(body)))
    result.status match {
      case s if s >= 200 && s < 300 => responseBuilder(result)
      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when writing to Github. Got status ${result.status}: ${result.body}"))
    }
  }

  def postJsonString(url:URL, body:String): Future[String] = {
    println("postJsonString url = " + url)
    buildJsonCall("POST", url, Some(Json.parse(body))).execute().flatMap { case result =>
      result.status match {
        case s if s >= 200 && s < 300 => Future.successful(result.body)
        case _@e => Future.failed(new scala.Exception(s"Didn't get expected status code when writing to Github. Got status ${result.status}: ${result.body}"))
      }
    }
//    result.status match {
//      case s if s >= 200 && s < 300 => responseBuilder(result)
//      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when writing to Github. Got status ${result.status}: ${result.body}"))
//    }
  }

  def postUnit(url:URL, body:JsValue): Try[Unit] = {
    post[Unit](_ => Success(Unit))(url, body)
  }
}
