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

package uk.gov.hmrc.initrepository.bintray

import java.net.URL

import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}
import uk.gov.hmrc.initrepository.{HttpTransport, Log, RequestException, ServiceCredentials}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class BintrayUrls(apiRoot:String = "https://bintray.com/api/v1"){
  def containsPackage(repoName: String, packageName: String):URL =
    new URL(s"$apiRoot/packages/hmrc/$repoName/$packageName")

  def createPackage(repoName: String):URL =
    new URL(s"$apiRoot/packages/hmrc/$repoName")
}

trait Bintray{

  def http:HttpTransport
  def urls:BintrayUrls

  def createPackage(repoName: String, packageName:String) :Future[Unit]={
    Log.info(s"creating Bintray package with name '$packageName' in repository '$repoName'")

    val req = http.buildJsonCallWithAuth(
      "POST",
      urls.createPackage(repoName),
      Some(Json.parse(buildCreatePackageMessage(repoName, packageName)))
    )

    req.execute() flatMap { res => res.status match {
      case 201 => Future.successful(Unit)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def buildCreatePackageMessage(repoName: String, packageName:String):String={
    s""" {
      |    "name": "$packageName",
      |    "desc": "$packageName $repoName",
      |    "labels": [],
      |    "licenses": ["Apache-2.0"],
      |    "vcs_url": "https://github.com/hmrc/$packageName",
      |    "website_url": "https://github.com/hmrc/$packageName",
      |    "issue_tracker_url": "https://github.com/hmrc/$packageName/issues",
      |    "github_repo": "hmrc/$packageName",
      |    "public_download_numbers": true,
      |    "public_stats": true
      |}""".stripMargin
  }

  def containsPackage(repoName: String, packageName:String): Future[Boolean] = {
    val req = http.buildJsonCallWithAuth("GET", urls.containsPackage(repoName, packageName))

    req.execute() flatMap { res => res.status match {
      case 200 => Future.successful(true)
      case 404 => Future.successful(false)
      case _   => Future.failed(new RequestException(req, res))
    }}
  }

  def close() = http.close()
}
