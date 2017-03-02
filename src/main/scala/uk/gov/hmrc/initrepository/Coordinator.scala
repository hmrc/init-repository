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

import uk.gov.hmrc.initrepository.FutureUtils.exponentialRetry
import uk.gov.hmrc.initrepository.RepositoryType.RepositoryType
import uk.gov.hmrc.initrepository.bintray.BintrayService
import uk.gov.hmrc.initrepository.git.LocalGitService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Coordinator(github: Github, bintray: BintrayService, git: LocalGitService, travis: TravisConnector) {

  type PreConditionError[T] = Option[T]

  def run(newRepoName: String, teamNames: Seq[String], repositoryType: RepositoryType, bootstrapVersion: String, enableTravis: Boolean): Future[Unit] = {
    checkPreConditions(newRepoName, teamNames).flatMap { error =>
      if (error.isEmpty) {
        Log.info(s"Pre-conditions met, creating '$newRepoName'")

        for {
          repoUrl <- initGitRepo(newRepoName, repositoryType, bootstrapVersion)
          _ <- bintray.createPackagesFor(newRepoName)
          _ <- initTravis(newRepoName, enableTravis)
          _ <- addTeamsToGitRepo(teamNames, newRepoName)
        } yield repoUrl
      } else {
        Future.failed(new Exception(s"pre-condition check failed with: ${error.get}"))
      }
    }.map { repoUrl =>
      val repoWebUrl = "https://github.com/hmrc/" + newRepoName
      Log.info(s"Successfully created $repoWebUrl")
    }
  }

  private def addTeamsToGitRepo(teamNames: Seq[String], newRepoName: String): Future[Seq[Unit]] = {

    val x: Seq[Future[Unit]] = teamNames.map { teamName =>
      val teamIdFuture: Future[Option[Int]] =  github.teamId(teamName)

      teamIdFuture.flatMap { teamId =>
        exponentialRetry(10) {
          addRepoToTeam(newRepoName, teamId)
        }
      }
    }
    Future.sequence(x)
  }


  private def initGitRepo(newRepoName: String, repositoryType: RepositoryType, bootstrapVersion: String): Future[String] = {
    for {
      repoUrl <- github.createRepo(newRepoName)
      _ <- tryToFuture(git.initialiseRepository(repoUrl, repositoryType, bootstrapVersion))
    } yield repoUrl
  }

  private def addRepoToTeam(repoName: String, teamIdO: Option[Int]): Future[Unit] = {
    teamIdO.map { teamId =>
      github.addRepoToTeam(repoName, teamId)
    }.getOrElse(Future.failed(new Exception("Didn't have a valid team id")))
  }

  private def initTravis(newRepoName: String, enable: Boolean): Future[Unit] = {
    implicit val backoffStrategy = TravisSearchBackoffStrategy()
    if (enable)
      for {
        authentication <- travis.authenticate
        _ <- travis.syncWithGithub(authentication.accessToken)
        newRepoId <- travis.searchForRepo(authentication.accessToken, newRepoName)
        _ <- travis.activateHook(authentication.accessToken, newRepoId)
      } yield Unit
    else{
      Log.info(s"Skiping travis intigration")
      Future.successful(())
    }
  }

  private def tryToFuture[A](t: => Try[A]): Future[A] = {
    Future {
      t
    }.flatMap {
      case Success(s) => Future.successful(s)
      case Failure(fail) => Future.failed(fail)
    }
  }

  case class Team(teamName: String, existsInGithub: Boolean)

  def getTeamsFromGithub(teamNames: Seq[String]): Future[Seq[Team]] = {
    Future.sequence(teamNames.map(team => github.teamId(team).map(id => Team(team, id.isDefined))))
  }


  private def checkPreConditions(newRepoName: String, teamNames: Seq[String]): Future[Option[String]] = {

      val repoExistsErrorFO: Future[Option[String]] = github.containsRepo(newRepoName).map(exists => if(exists) Some("Error1") else None)
      val packageExistsErrorFO: Future[Option[String]] = bintray.reposContainingPackage(newRepoName).map(existingPack => if(existingPack.nonEmpty) Some("Error 2") else None )

      val teamMissingErrorFO: Future[Option[String]] =
        getTeamsFromGithub(teamNames)
          .map { teams =>
            teams.filterNot(_.existsInGithub)
              .map(t => s"Error $t")
              .reduceOption(_ + ", " + _)
          }

      Future
        .sequence(Seq(repoExistsErrorFO, packageExistsErrorFO, teamMissingErrorFO))
        .map(_.flatten)
        .map(errors => if (errors.isEmpty) None else Some(errors.mkString(", ")))
    }


  def missingTeamErrorMessage(teamName: String) = {
    s"Team with name '$teamName' could not be found in github"
  }
}
