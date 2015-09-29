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

package git

import java.io.File
import java.nio.file.{Paths, Files, Path}

import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Git{
  def sshUrl(name:String, orgName:String) = s"git@github.com:$orgName/$name.git".toLowerCase

}

class LocalGitStore(workspace:Path) {


  val orgName = "HMRC"

  val gitCommand = Await.result(Command.run("which git"), 5.seconds).head.trim

  git("--version").map{ _.headOption.getOrElse("<no output from git>")}.foreach { version =>
    println(s"using git version $version")
  }

  def commitFileToRoot(repoName: String, fileName:String, fileContent: String): Future[Unit]= {
    val target: Path = workspace.resolve(repoName).resolve(fileName)
    if (!target.toFile.exists()) {
      target.toFile.createNewFile()
    }

    println(s"writing to target = $target")
    Files.write(target, fileContent.getBytes("UTF-8"))
    git(s"add .", inRepo = Some(repoName)).flatMap { r =>
      gitCommandParts(Array("commit", "-minitial"), inRepo = Some(repoName)).map { _ => Unit }
    }
  }

  def push(repoName:String): Future[Unit] ={
    Command.run(s"$gitCommand push origin master", inDir = Some(workspace.resolve(repoName))).map{o => println(o); o}.map { _ => Unit }
  }

  def commitCount(repoName:String):Future[Int]={
    git("log", inRepo = Some(repoName)).map(_.foreach(println))
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

  def init(name:String):Future[Unit]={
    git(s"init $name").map { _ => Unit }
  }

  def cloneRepoURL(url: String): Future[Unit] = {
    val name = url.split('/').last.stripSuffix(".git")
    val targetDir = workspace.resolve(name)

    println(s"targetDir = $targetDir")

    while(targetDir.toFile.exists()) {
      println(s"removing $targetDir")
      FileUtils.deleteDirectory(targetDir.toFile)
    }

    val resultF = git(s"clone $url")

    resultF.onComplete {
      x => println("Completed cloning " + name)
    }
    resultF map { _ => Unit }
  }
}
