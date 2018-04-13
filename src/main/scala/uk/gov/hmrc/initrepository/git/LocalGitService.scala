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

package uk.gov.hmrc.initrepository.git

import uk.gov.hmrc.initrepository.RepositoryType.RepositoryType
import uk.gov.hmrc.initrepository.bintray.BintrayConfig

import scala.util.{Failure, Try}

class LocalGitService(git: LocalGitStore) {

  val BootstrapTagComment = "Bootstrap tag"
  val BootstrapTagVersion: String => String = version => s"v$version"

  val TravisScalaVersion = "2.11.6"
  val TravisJdkVersion = "oraclejdk8"

  val CommitUserName = "hmrc-web-operations"
  val CommitUserEmail = "hmrc-web-operations@digital.hmrc.gov.uk"

  def buildReadmeTemplate(repoName:String, repositoryType:RepositoryType, enableTravis: Boolean, privateRepo: Boolean):String={
    val bintrayRepoName = BintrayConfig.releasesRepositoryNameFor(repositoryType)

    if (privateRepo)
      s"""
         |# $repoName
         |
         |This is a placeholder README.md for a new repository
         |""".stripMargin
    else
      s"""
        |# $repoName
        |
        |${if(enableTravis){s"[![Build Status](https://travis-ci.org/hmrc/$repoName.svg?branch=master)](https://travis-ci.org/hmrc/$repoName)"} else ""} [ ![Download](https://api.bintray.com/packages/hmrc/$bintrayRepoName/$repoName/images/download.svg) ](https://bintray.com/hmrc/$bintrayRepoName/$repoName/_latestVersion)
        |
        |This is a placeholder README.md for a new repository
        |
        |### License
        |
        |This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
        |""".stripMargin
  }

  def buildTravisYamlTemplate(repoName:String):String={
    s"""
      |sudo: false
      |language: scala
      |scala:
      |- $TravisScalaVersion
      |jdk:
      |- $TravisJdkVersion
      |cache:
      |  directories:
      |    - '$$HOME/.ivy2/cache'
      |branches:
      |  except:
      |    - master
    """.stripMargin
  }

  val gitIgnoreContents = {
    """
      |logs
      |project/project
      |project/target
      |target
      |lib_managed
      |tmp
      |.history
      |dist
      |/.idea
      |/*.iml
      |/*.ipr
      |/out
      |/.idea_modules
      |/.classpath
      |/.project
      |/RUNNING_PID
      |/.settings
      |*.iws
      |node_modules/
      |npm-debug.log
      |yarn-debug.log
      |yarn-error.log
      |
    """.stripMargin
  }



  def initialiseRepository(repoUrl: String, repositoryType: RepositoryType, bootstrapVersion: String, digitalServiceName: Option[String], enableTravis: Boolean, privateRepo: Boolean): Try[Unit] = {

    def getManifestContents(digitalServiceName: Option[String]) = digitalServiceName.map(dsn => s"digital-service: $dsn")

    val newRepoName = repoUrl.split('/').last.stripSuffix(".git")
    for {
      _    <- git.cloneRepoURL(repoUrl)
      _    <- if(enableTravis) git.commitFileToRoot(newRepoName, ".travis.yml", buildTravisYamlTemplate(newRepoName), CommitUserName, CommitUserEmail) else Try(Unit)
      _    <- git.commitFileToRoot(newRepoName, ".gitignore", gitIgnoreContents, CommitUserName, CommitUserEmail)
      _    <- git.commitFileToRoot(newRepoName, "README.md", buildReadmeTemplate(newRepoName, repositoryType, enableTravis, privateRepo), CommitUserName, CommitUserEmail)
      _    <- git.commitFileToRoot(newRepoName, "repository.yaml", getManifestContents(digitalServiceName), CommitUserName, CommitUserEmail)
      _    <- git.push(newRepoName)
      shaO <- if(!privateRepo) git.lastCommitSha(newRepoName) else Try(None)
      _    <- if(!privateRepo) maybeCreateTag(newRepoName, shaO, BootstrapTagComment, BootstrapTagVersion(bootstrapVersion)) else Try(Unit)
      _    <- if(!privateRepo) git.pushTags(newRepoName) else Try(Unit)
    } yield Unit
  }

  def maybeCreateTag(newRepoName: String, shaOpt: Option[String], tagText: String, version: String): Try[Unit] = {
    shaOpt.map { sha =>
      git.tagAnnotatedCommit(newRepoName, sha, tagText, version)
    }.getOrElse {
      Failure(new IllegalAccessException("Didn't get a valid sha, check the list of commits"))
    }
  }
}
