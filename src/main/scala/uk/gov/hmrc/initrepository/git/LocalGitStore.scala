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

package uk.gov.hmrc.initrepository.git

import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils
import uk.gov.hmrc.initrepository.Log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Git{
  def sshUrl(name:String, orgName:String) = s"git@github.com:$orgName/$name.git".toLowerCase

}

class LocalGitStore(workspace:Path) {

  val orgName = "HMRC"

  val gitCommand = Await.result(Command.run("which git"), 5.seconds).head.trim

  git("--version").map{ _.headOption.getOrElse("<no output from git>")}.foreach { version =>
    Log.info(s"using git CLI version $version")
  }

  def lastCommitSha(repoName: String):Future[Option[String]]={
    git("rev-parse HEAD", inRepo = Some(repoName)).map(_.headOption.map(_.trim()))
  }

  def lastCommitUser(repoName: String):Future[Option[String]]={
    git("log --pretty=format:%an", inRepo = Some(repoName)).map(_.headOption.map(_.trim()))
  }

  def lastTag(repoName: String):Future[Option[String]]={
    git("describe --abbrev=0", inRepo = Some(repoName)).map(_.headOption.map(_.trim()))
  }

  def tagAnnotatedCommit(repoName: String, sha: String, tag:String, version:String):Future[Unit] = {
    val versionWithPrefix = "v" + version.stripPrefix("v")
    gitCommandParts(Array("tag", "-a", "-m", "Bootstrap tag",  versionWithPrefix), inRepo = Some(repoName)).map { _ => Unit }
  }

  def commitFileToRoot(repoName: String, fileName:String, fileContent: String, user:String, email:String): Future[Unit]= {
    val target: Path = workspace.resolve(repoName).resolve(fileName)
    if (!target.toFile.exists()) {
      target.toFile.createNewFile()
    }

    Files.write(target, fileContent.getBytes("UTF-8"))

    git(s"add .", inRepo = Some(repoName)).flatMap { r =>
      gitCommandParts(Array(
        "-c", s"user.email=$email",
        "-c", s"user.name=$user",
        "commit",
        s"""-madding $fileName"""
      ), inRepo = Some(repoName)).map { _ => Unit }
    }
  }

  def push(repoName:String): Future[Unit] ={
    Command.run(s"$gitCommand push origin master", inDir = Some(workspace.resolve(repoName))).map { _ => Unit }
  }

  def pushTags(repoName:String): Future[Unit] ={
    Command.run(s"$gitCommand push --tags origin master", inDir = Some(workspace.resolve(repoName))).map { _ => Unit }
  }

  def commitCount(repoName:String):Future[Int]={
    git("rev-list HEAD --count", inRepo = Some(repoName)).map(_.head.trim.toInt)
  }

  def gitCommandParts(commandParts:Array[String], inRepo:Option[String] = None):Future[List[String]]={
    val cwd = inRepo.map(r => workspace.resolve(r)).getOrElse(workspace)
    Command.runArray(gitCommand +: commandParts, inDir = Some(cwd))
  }

  def git(command:String, inRepo:Option[String] = None):Future[List[String]]={
    val cwd = inRepo.map(r => workspace.resolve(r)).getOrElse(workspace)
    Command.run(s"$gitCommand $command", inDir = Some(cwd))
  }

  def init(name:String, isBare:Boolean = false):Future[Unit]={
    val bareFlag = if(isBare) "--bare " else ""
    git(s"init $bareFlag$name").map { _ => Unit }
  }

  def cloneRepoURL(url: String): Future[Unit] = {
    val name = url.split('/').last.stripSuffix(".git")
    val targetDir = workspace.resolve(name)

    Log.info(s"cloning $url into $targetDir")

    while(targetDir.toFile.exists()) {
      FileUtils.deleteDirectory(targetDir.toFile)
    }

    git(s"clone $url") map { _ => Unit }
  }
}
