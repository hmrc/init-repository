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

import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}
import play.api.libs.ws.{WSClientConfig, WSAuthScheme, WSResponse}
import play.api.mvc.Results

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


class BintrayUrls(apiRoot:String = "https://bintray.com/api/v1"){
  def containsRepoUrl(org: String, repo: String):URL = ???
}

class Bintray(bintrayHttp:BintrayHttp, bintrayUrls:BintrayUrls){

  def createRepo(repoName: String) = ???

  val log = new Logger()

  def containsRepo(repo: String): Try[Boolean] = {
    //val url = BintrayUrls.containsRepoUrl(org, repo)
    ???
  }
}


trait BintrayHttp{

  def creds:ServiceCredentials

  val log = new Logger()

  val ws = new NingWSClient(new NingAsyncHttpClientConfigBuilder(new WSClientConfig()).build())


  def apiWs(url:String) = ws.url(url)
    .withAuth(
      creds.user, creds.pass, WSAuthScheme.BASIC)
    .withHeaders("content-type" -> "application/json")

  def emptyPost(url:String): Try[Unit] = {
    log.info(s"posting file to $url")

    val call = apiWs(url).post(Results.EmptyContent())

    val result: WSResponse = Await.result(call, Duration.apply(5, TimeUnit.MINUTES))

    //log.info(s"result ${result.status} - ${result.statusText}")

    result.status match {
      case s if s >= 200 && s < 300 => Success(new URL(url))
      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when writing to Bintray. Got status ${result.status}: ${result.body}"))
    }
  }

  def get[A](url:String): Try[String] ={
    log.info(s"getting file from $url")

    val call = apiWs(url).get()

    val result: WSResponse = Await.result(call, Duration.apply(5, TimeUnit.MINUTES))

    //log.info(s"result ${result.status} - ${result.statusText} - ${result.body}")

    result.status match {
      case s if s >= 200 && s < 300 => Success(result.body)
      case _@e => Failure(new scala.Exception(s"Didn't get expected status code when writing to Bintray. Got status ${result.status}: ${result.body}"))
    }
  }

}
