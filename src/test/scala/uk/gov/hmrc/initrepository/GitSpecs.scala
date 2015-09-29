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

import java.nio.file.{Files, Path, Paths}

import git.LocalGitStore
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import scala.collection.JavaConversions._

class GitSpecs extends WordSpec with Matchers with FutureValues with BeforeAndAfterEach{

  val thisProjectsPath = Paths.get(".").toAbsolutePath.normalize()
  val thisRepoName = thisProjectsPath.getFileName.toString

  var tempDir:Path = _
  var git:LocalGitStore = _

  override def beforeEach(): Unit ={
    tempDir = Files.createTempDirectory("init-repository-git-store-").toAbsolutePath
    println(s"tempDir = $tempDir")
    git = new LocalGitStore(tempDir)
  }


  "Git.cloneRepo" should {

    "clone a repo" in {
      git.cloneRepoURL(thisProjectsPath.toString).await
      
      tempDir.resolve(thisRepoName).resolve(".git").toFile.isDirectory shouldBe true
    }
  }
  
  def cloneThisRepo(git:LocalGitStore, targetDir:Path): Unit ={
    git.cloneRepoURL(thisProjectsPath.toString).await
  }

  "Git.commitFileToRoot" should {

    "commit a file with given contents to the repository root when the file doesn't exist" in {
      git.init("test-repo").await

      git.commitFileToRoot("test-repo", "README.MD", fileContent = "Some useful info.").await

      git.commitCount("test-repo").await shouldBe 1
      Files.readAllLines(tempDir.resolve("test-repo").resolve("README.MD")).head.trim shouldBe "Some useful info."
    }
  }
}
