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

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._



object Main {

  val githubCredsFile  = System.getProperty("user.home") + "/.github/.credentials"
  val bintrayCredsFile = System.getProperty("user.home") + "/.bintray/.credentials"

  val githubCredsOpt  = CredentialsFinder.findGithubCredsInFile(new File(githubCredsFile).toPath)
  val bintrayCredsOpt = CredentialsFinder.findBintrayCredsInFile(new File(bintrayCredsFile).toPath)

  private val githubHttp: GithubHttp with Object {def creds: ServiceCredentials} = new GithubHttp {
    override def creds: ServiceCredentials = githubCredsOpt.get

    Log.debug(s"github client_id ${creds.user}")
    Log.debug(s"github client_secret ${creds.pass.takeRight(3)}*******")

  }
  val github  = new Github(githubHttp, new GithubUrls())

  private val bintrayHttp: BintrayHttp with Object {def creds: ServiceCredentials} = new BintrayHttp {
    override def creds: ServiceCredentials = bintrayCredsOpt.get

    Log.debug(s"bintrayCredsOpt client_id ${creds.user}")
    Log.debug(s"bintrayCredsOpt client_secret ${creds.pass.takeRight(3)}*******")
  }
  val bintray = new Bintray(bintrayHttp, new BintrayUrls())

  implicit class FuturePimp[T](self:Future[T]){
    def await:Future[T] = {
      Await.result(self, 30 seconds)
      self
    }
  }


  type PreConditionError[T] = Option[T]

  def main(args: Array[String]) {

    if(args.length != 2)
      throw new IllegalArgumentException("need 2 items")

    val newRepoName = args(0)
    val team = args(1)

    try {
      val preConditions: Future[PreConditionError[String]] = checkPreConditions(newRepoName, team)

      val result: Future[Unit] = preConditions.flatMap { error =>
        if (error.isEmpty) {
          Log.info(s"Pre-conditions met, creating '$newRepoName'")
          for (_ <- github.createRepo(newRepoName).await;
               _ <- bintray.createPackage("releases", newRepoName);
               _ <- bintray.createPackage("release-candidates", newRepoName);
               teamIdO <- github.teamId(team);
               _ <- addRepoToTeam(newRepoName, teamIdO)
          ) yield ()
        } else {
          Future.failed(new Exception(s"pre-condition check failed with: ${error.get}"))
        }
      }

      Await.result(result, Duration(30, TimeUnit.SECONDS))
      Log.info("complete.")

    } finally {
      bintrayHttp.close()
      githubHttp.close()
    }
  }

  def addRepoToTeam(repoName:String, teamIdO:Option[Int]):Future[Unit]={
    teamIdO.map { teamId =>
      github.addRepoToTeam(repoName, teamIdO.get)
    }.getOrElse(Future.failed(new Exception("Didn't have a valid team id")))
  }


  def checkPreConditions(newRepoName:String, team:String):Future[PreConditionError[String]]  ={
    for(repoExists  <- github.containsRepo(newRepoName);
        releasesExists  <- bintray.containsPackage("releases", newRepoName);
        releaseCandExists <- bintray.containsPackage("release-candidates", newRepoName);
        teamExists <- github.teamId(team).map(_.isDefined))
      yield{
        if (repoExists)  Some(s"Repository with name '$newRepoName' already exists in github ")
        else if (releasesExists)  Some(s"Package with name '$newRepoName' already exists in bintray releases")
        else if (releaseCandExists) Some(s"Package with name '$newRepoName' already exists in bintray release-candidates")
        else if (!teamExists) Some(s"Team with name '$team' could not be found in github")
        else None
      }
  }

}
