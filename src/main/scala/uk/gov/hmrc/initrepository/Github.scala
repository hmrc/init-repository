/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json._
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GithubUrls(orgName: String = "hmrc", apiRoot: String = "https://api.github.com") {

  val PAGE_SIZE = 100

  def createRepo: URL =
    new URL(s"$apiRoot/orgs/$orgName/repos")

  def containsRepo(repo: String) =
    new URL(s"$apiRoot/repos/$orgName/$repo")

  def teams(page: Int = 1) =
    new URL(s"$apiRoot/orgs/$orgName/teams?per_page=$PAGE_SIZE&page=$page")

  def addTeamToRepo(repoName: String, teamId: Int) =
    new URL(s"$apiRoot/teams/$teamId/repos/$orgName/$repoName?permission=push")

  def addBranchProtection(repo: String, branch: String): URL =
    new URL(branchProtectionRoot(repo, branch))

  def addRequireSignedCommits(repo: String, branch: String): URL =
    new URL(s"${branchProtectionRoot(repo, branch)}/required_signatures")

  private def branchProtectionRoot(repo: String, branch: String): String = {
    s"$apiRoot/repos/$orgName/$repo/branches/$branch/protection"
  }
}

trait Github {

  def httpTransport: HttpTransport

  def githubUrls: GithubUrls

  val IronManApplication = "application/vnd.github.ironman-preview+json"
  val ZzzaxApplication = "application/vnd.github.zzzax-preview+json"
  val LukeCageApplication = "application/vnd.github.luke-cage-preview+json"

  def teamId(teamName: String): Future[Option[Int]] =
    allTeams().map { teams =>
      teams.find(_.name == teamName).map(_.id)
    }

  private def allTeams(page: Int = 1): Future[Seq[Team]] = {

    implicit val format = Json.format[Team]

    val req = httpTransport.buildJsonCallWithAuth("GET", githubUrls.teams(page))

    val aPageOfTeams = req.execute().flatMap { res =>
      res.status match {
        case 200 => Future.successful(res.json.as[Seq[Team]])
        case _   => Future.failed(new RequestException(req, res))
      }
    }

    aPageOfTeams.flatMap { currentPage =>
      if (currentPage.size == githubUrls.PAGE_SIZE) {
        allTeams(page + 1).map { nextPage =>
          currentPage ++ nextPage
        }
      } else Future.successful(currentPage)
    }
  }

  def addRepoToTeam(repoName: String, teamId: Int, permission: String): Future[Unit] = {
    Log.info(s"Adding $repoName to team $teamId")

    val req = httpTransport
      .buildJsonCallWithAuth("PUT", githubUrls.addTeamToRepo(repoName, teamId))
      .withHeaders("Accept" -> IronManApplication)
      .withHeaders("Content-Length" -> "0")
      .withBody(s"""{"permission": "$permission"}"""")

    Log.debug(req.toString)

    req.execute().flatMap { res =>
      res.status match {
        case 204 => Future.successful(Unit)
        case _   => Future.failed(new RequestException(req, res))
      }
    }
  }

  def updateDefaultBranch(repoName: String, defaultBranchName: String): Future[Unit] = {
    Log.info(s"Updating default branch name for $repoName to $defaultBranchName")

    val req = httpTransport
      .buildJsonCallWithAuth("PATCH", githubUrls.containsRepo(repoName))
      .withHeaders("Accept" -> IronManApplication)
      .withHeaders("Content-Length" -> "0")
      .withBody(s"""{"default_branch": "$defaultBranchName"}"""")

    Log.debug(req.toString)

    req.execute().flatMap { res =>
      res.status match {
        case 200 => Future.successful(Unit)
        case _   => Future.failed(new RequestException(req, res))
      }
    }
  }

  def containsRepo(repoName: String): Future[Boolean] = {
    val req = httpTransport.buildJsonCallWithAuth("GET", githubUrls.containsRepo(repoName))

    Log.debug(req.toString)

    req.execute().flatMap { res =>
      res.status match {
        case 200 => Future.successful(true)
        case 301 => Future.successful(false)
        case 404 => Future.successful(false)
        case _   => Future.failed(new RequestException(req, res))
      }
    }
  }

  def createRepo(repoName: String, privateRepo: Boolean): Future[String] = {
    Log.info(s"creating github repository with name '$repoName'")
    val payload = s"""{
                    |    "name": "$repoName",
                    |    "description": "",
                    |    "homepage": "",
                    |    "private": $privateRepo,
                    |    "has_issues": true,
                    |    "has_wiki": true,
     ${if (!privateRepo) """"license_template": "apache-2.0",""" else ""}
                    |    "has_downloads": true
                    |}""".stripMargin

    val url = githubUrls.createRepo
    val req = httpTransport.buildJsonCallWithAuth("POST", url, Some(Json.parse(payload)))

    Log.debug(req.toString)

    req.execute().flatMap {
      case result =>
        result.status match {
          case s if s >= 200 && s < 300 => Future.successful(s"https://github.com/hmrc/$repoName")
          case _ @e =>
            Future.failed(new scala.Exception(
              s"Didn't get expected status code when writing to $url. Got status ${result.status}: POST $url ${result.body}"))
        }
    }
  }

  def addRequireSignedCommits(repoName: String, branches: Seq[String]): Future[String] = {
    branches match {
      case Nil => Future.successful(s"Repo $repoName does not require signed commits")
      case _   => Future.sequence(branches.map { branch =>
        for {
          _      <- addBranchProtection(repoName, branch)
          result <- addRequireSignedCommitsToBranch(repoName, branch)
        } yield {
          result
        }
      }).map(_.mkString(", "))
    }
  }

  private def addBranchProtection(repoName: String, branch: String) = {
    Log.info(s"Adding branch protection to repo $repoName for branch $branch")
    val url = githubUrls.addBranchProtection(repoName, branch)

    val payload = s"""{
                     |    "required_status_checks": null,
                     |    "enforce_admins": false,
                     |    "required_pull_request_reviews": null,
                     |    "restrictions": null,
                     |    "required_linear_history": false,
                     |    "allow_force_pushes": true,
                     |    "allow_deletions": false
                     |}""".stripMargin

    val request: WSRequest = httpTransport
      .buildJsonCallWithAuth("PUT", url, Some(Json.parse(payload)))
      .withHeaders(("Accept", LukeCageApplication))

    Log.debug(request.toString)

    request.execute().flatMap { result =>
      result.status match {
        case 200 =>
          Future.successful(s"Enabled branch protection for repo $repoName on branch $branch")
        case _ =>
          Future.failed(new Exception(s"Didn't get expected status code when writing to $url. Got status ${result.status}: POST $url ${result.body}"))
      }
    }
  }

  private def addRequireSignedCommitsToBranch(repoName: String, branch: String): Future[String] = {
    Log.info(s"Adding require signed commits to repo $repoName for branch $branch")
    val url = githubUrls.addRequireSignedCommits(repoName, branch)

    val request: WSRequest = httpTransport
      .buildJsonCallWithAuth("POST", url, None)
      .withHeaders(("Accept", ZzzaxApplication))

    Log.debug(request.toString)

    request.execute().flatMap { result =>
      result.status match {
        case 200 =>
          Future.successful(s"Enabled require signed commits for repo $repoName on branch $branch")
        case _ =>
          Future.failed(new Exception(s"Didn't get expected status code when writing to $url. Got status ${result.status}: POST $url ${result.body}"))
      }
    }
  }

  def close() = httpTransport.close()
}

case class SimpleResponse(status: Int, rawBody: String)

case class Team(name: String, id: Int)

class RequestException(request: WSRequest, response: WSResponse)
    extends Exception(s"Got status ${response.status}: ${request.method} ${request.url} ${response.body}")
