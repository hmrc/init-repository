/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.initrepository.FutureUtils.exponentialRetry
import uk.gov.hmrc.initrepository.git.LocalGitService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Coordinator(github: Github, git: LocalGitService) {

  type PreConditionError[T] = Option[T]

  def run(
    newRepoName: String,
    teams: Seq[String],
    digitalServiceName: Option[String],
    bootstrapTag: Option[String],
    privateRepo: Boolean,
    githubToken: String,
    requireSignedCommits: Seq[String]): Future[Unit] =
    checkPreConditions(newRepoName, teams, privateRepo)
      .flatMap { error =>
        if (error.isEmpty) {
          Log.info(s"Pre-conditions met, creating '$newRepoName'")
          for {
            repoUrl <- github.createRepo(newRepoName, privateRepo)
            _       <- addTeamsToGitRepo(teams, newRepoName)
            _       <- addRepoAdminsTeamToGitRepo(newRepoName)
            _       <- tryToFuture(
                          git.initialiseRepository(newRepoName, digitalServiceName, bootstrapTag, privateRepo, githubToken))
            _       <- github.addRequireSignedCommits(newRepoName, requireSignedCommits)
          } yield repoUrl
        } else {
          Future.failed(new Exception(s"pre-condition check failed with: ${error.get}"))
        }
      }
      .map { repoUrl =>
        Log.info(s"Successfully created $repoUrl")
      }

  private def addTeamsToGitRepo(teamNames: Seq[String], newRepoName: String): Future[Seq[Unit]] = {

    val x: Seq[Future[Unit]] = teamNames.map { teamName =>
      val teamIdFuture: Future[Option[Int]] = github.teamId(teamName)

      teamIdFuture.flatMap { teamId =>
        exponentialRetry(10) {
          addRepoToTeam(newRepoName, teamId, "push")
        }
      }
    }
    Future.sequence(x)
  }

  private def addRepoAdminsTeamToGitRepo(newRepoName: String): Future[Unit] = {

    val teamName                          = "Repository Admins"
    val teamIdFuture: Future[Option[Int]] = github.teamId(teamName)

    teamIdFuture.flatMap { teamId =>
      exponentialRetry(10) {
        addRepoToTeam(newRepoName, teamId, "admin")
      }
    }
  }

  private def addRepoToTeam(repoName: String, teamIdO: Option[Int], permission: String): Future[Unit] =
    teamIdO
      .map { teamId =>
        github.addRepoToTeam(repoName, teamId, permission)
      }
      .getOrElse(Future.failed(new Exception("Didn't have a valid team id")))

  private def tryToFuture[A](t: => Try[A]): Future[A] =
    Future {
      t
    }.flatMap {
      case Success(s)    => Future.successful(s)
      case Failure(fail) => Future.failed(fail)
    }

  def checkTeamsExistOnGithub(teamNames: Seq[String]): Future[Boolean] =
    Future.sequence(teamNames.map(team => github.teamId(team))).map(_.flatten).map(_.size == teamNames.size)

  private def checkPreConditions(
    newRepoName: String,
    teams: Seq[String],
    privateRepo: Boolean): Future[PreConditionError[String]] =
    for {
      repoExists <- github.containsRepo(newRepoName)
      teamsExist <- checkTeamsExistOnGithub(teams)
    } yield {
      if (repoExists) Some(s"Repository with name '$newRepoName' already exists in github ")
      else if (!teamsExist)
        Some(s"One of the provided team names ('${teams.mkString(",")}') could not be found in github")
      else None
    }
}
