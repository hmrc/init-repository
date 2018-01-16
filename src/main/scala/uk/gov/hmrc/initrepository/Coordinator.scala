/*
 * Copyright 2018 HM Revenue & Customs
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

  def run(newRepoName: String, teams: Seq[String], repositoryType: RepositoryType, bootstrapVersion: String, enableTravis: Boolean, digitalServiceName: Option[String], privateRepo: Boolean): Future[Unit] = {
    checkPreConditions(newRepoName, teams, privateRepo).flatMap { error =>
      if (error.isEmpty) {
        Log.info(s"Pre-conditions met, creating '$newRepoName'")

        for {
          repoUrl <- initGitRepo(newRepoName, teams, repositoryType, bootstrapVersion, digitalServiceName, enableTravis, privateRepo)
          _       <- initBintray(newRepoName, privateRepo)
          _       <- initTravis(newRepoName, enableTravis)
        } yield repoUrl

      } else {
        Future.failed(new Exception(s"pre-condition check failed with: ${error.get}"))
      }
    }.map { repoUrl =>
      val repoWebUrl = "https://github.com/hmrc/" + newRepoName
      Log.info(s"Successfully created $repoWebUrl")
    }
  }

  private def initGitRepo(newRepoName: String, teams: Seq[String], repositoryType: RepositoryType, bootstrapVersion: String, digitalServiceName: Option[String], enableTravis: Boolean, privateRepo: Boolean): Future[String] =
    for {
      repoUrl <- github.createRepo(newRepoName, privateRepo)
      _ <- addTeamsToGitRepo(teams, newRepoName)
      _ <- tryToFuture(git.initialiseRepository(repoUrl, repositoryType, bootstrapVersion, digitalServiceName, enableTravis, privateRepo))
    } yield repoUrl

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

  private def addRepoToTeam(repoName: String, teamIdO: Option[Int]): Future[Unit] = {
    teamIdO.map { teamId =>
      github.addRepoToTeam(repoName, teamIdO.get)
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
      Log.info(s"Skipping travis integration")
      Future.successful(())
    }
  }

  private def initBintray(newRepoName: String, privateRepo: Boolean): Future[Unit] = {
    if(privateRepo) {
      Log.info(s"Skipping Bintray packages creation as this is a private repository")
      Future.successful()
    } else bintray.createPackagesFor(newRepoName)
  }

  private def tryToFuture[A](t: => Try[A]): Future[A] = {
    Future {
      t
    }.flatMap {
      case Success(s) => Future.successful(s)
      case Failure(fail) => Future.failed(fail)
    }
  }

  def checkTeamsExistOnGithub(teamNames: Seq[String]): Future[Boolean] = {
    Future.sequence(teamNames.map(team => github.teamId(team))).map(_.flatten).map(_.size == teamNames.size)
  }

  private def checkPreConditions(newRepoName: String, teams: Seq[String], privateRepo: Boolean): Future[PreConditionError[String]] = {
    for {
      repoExists <- github.containsRepo(newRepoName)
      existingPackages <- if (privateRepo) Future.successful(Set.empty) else bintray.reposContainingPackage(newRepoName)
      teamsExist <- checkTeamsExistOnGithub(teams)
    } yield {
        if (repoExists) Some(s"Repository with name '$newRepoName' already exists in github ")
        else if (existingPackages.nonEmpty) Some(s"The following bintray packages already exist: '${existingPackages.mkString(",")}'")
        else if (!teamsExist) Some(s"One of the provided team names ('${teams.mkString(",")}') could not be found in github")
        else None
      }
  }
}
