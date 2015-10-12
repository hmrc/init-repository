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

import uk.gov.hmrc.initrepository.git.LocalGitService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Coordinator(github:Github, bintray: BintrayService, git:LocalGitService){

  import ImplicitPimps._

  type PreConditionError[T] = Option[T]

  def run(newRepoName:String, team:String):Future[Unit]= {

    checkPreConditions(newRepoName, team).flatMap { error =>
      if (error.isEmpty) {
        Log.info(s"Pre-conditions met, creating '$newRepoName'")
        for (repoUrl <- github.createRepo(newRepoName).await;
             _ <- bintray.createPackagesFor(newRepoName);
             teamIdO <- github.teamId(team);
             _ <- addRepoToTeam(newRepoName, teamIdO);
             _ <- git.initialiseRepository(repoUrl)
        ) yield repoUrl
      } else {
        Future.failed(new Exception(s"pre-condition check failed with: ${error.get}"))
      }
    }.map { repoUrl =>
      val repoWebUrl = "https://github.com/hmrc/" + newRepoName
      Log.info(s"Successfully created $repoWebUrl")
    }
  }

  def addRepoToTeam(repoName:String, teamIdO:Option[Int]):Future[Unit]={
    teamIdO.map { teamId =>
      github.addRepoToTeam(repoName, teamIdO.get)
    }.getOrElse(Future.failed(new Exception("Didn't have a valid team id")))
  }


  def checkPreConditions(newRepoName:String, team:String):Future[PreConditionError[String]]  ={
    for(repoExists  <- github.containsRepo(newRepoName);
        existingPackages <- bintray.reposContainingPackage(newRepoName);
        teamExists <- github.teamId(team).map(_.isDefined))
      yield{
        if (repoExists)  Some(s"Repository with name '$newRepoName' already exists in github ")
        else if (existingPackages.nonEmpty)  Some(s"The following bintray packages already exist: '${existingPackages.mkString(",")}'")
        else if (!teamExists) Some(s"Team with name '$team' could not be found in github")
        else None
      }
  }

}
