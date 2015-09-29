package uk.gov.hmrc.initrepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class Coordinator(github:Github, bintray: Bintray){

  type PreConditionError[T] = Option[T]

  def run(newRepoName:String, team:String):Future[Unit]= {

    val preConditions: Future[PreConditionError[String]] = checkPreConditions(newRepoName, team)

    preConditions.flatMap { error =>
      println(s"error = $error")
      if (error.isEmpty) {
        Log.info(s"Pre-conditions met, creating '$newRepoName'")
        for (repoUrl <- github.createRepo(newRepoName).await;
             _ <- bintray.createPackage("releases", newRepoName);
             _ <- bintray.createPackage("release-candidates", newRepoName);
             teamIdO <- github.teamId(team);
             _ <- addRepoToTeam(newRepoName, teamIdO)
        //               _ <- git.cloneRepoURL("git@github.com:hmrc/test-repo-1.git").await;
        //               sha <- git.commitFileToRoot(newRepoName, "README.MD", "Put useful info here").await;
        //               _ <- git.tagCommit(sha)
        //               _ <- git.push(newRepoName).await
        ) yield ()
      } else {
        Future.failed(new Exception(s"pre-condition check failed with: ${error.get}"))
      }
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


  implicit class FuturePimp[T](self:Future[T]){
    def await:Future[T] = {
      Await.result(self, 30 seconds)
      self
    }
  }
}
