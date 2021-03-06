/*
 * Copyright 2021 HM Revenue & Customs
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

import scala.util.{Failure, Try}

class LocalGitService(git: LocalGitStore) {

  val BootstrapTagComment                   = "Bootstrap tag"
  val BootstrapTagVersion: String => String = version => s"v$version"

  val CommitUserName  = "hmrc-web-operations"
  val CommitUserEmail = "hmrc-web-operations@digital.hmrc.gov.uk"

  def buildReadmeTemplate(repoName: String, privateRepo: Boolean): String =
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
        |This is a placeholder README.md for a new repository
        |
        |### License
        |
        |This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
        |""".stripMargin

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

  def buildRepositoryYaml(digitalServiceName: Option[String], privateRepo:Boolean) = {

    val publicVisibilityIdentifier  = "public_0C3F0CE3E6E6448FAD341E7BFA50FCD333E06A20CFF05FCACE61154DDBBADF71"
    val privateVisibilityIdentifier = "private_12E5349CFB8BBA30AF464C24760B70343C0EAE9E9BD99156345DD0852C2E0F6F"
    val visibilityIdentifier = if (privateRepo) {
      privateVisibilityIdentifier
    } else {
      publicVisibilityIdentifier
    }

    val repoVisibilityLine = s"repoVisibility: $visibilityIdentifier"

    val  digitalServiceNameLine = digitalServiceName.map(dsn => s"digital-service: $dsn")

    val lines = List(repoVisibilityLine) ++ digitalServiceNameLine
    lines.mkString("\n")

  }


  def initialiseRepository(

    newRepoName: String,
    digitalServiceName: Option[String],
    bootstrapTag: Option[String],
    privateRepo: Boolean,
    githubToken: String): Try[Unit] = {

    val url = s"https://$githubToken@github.com/hmrc/$newRepoName"
    for {
      _ <- git.cloneRepoURL(url)
      _ <- git.commitFileToRoot(newRepoName, ".gitignore", gitIgnoreContents, CommitUserName, CommitUserEmail)
      _ <- git.commitFileToRoot(
            newRepoName,
            "README.md",
            buildReadmeTemplate(newRepoName, privateRepo),
            CommitUserName,
            CommitUserEmail)
      _ <- git.commitFileToRoot(
            newRepoName,
            "repository.yaml",
            buildRepositoryYaml(digitalServiceName,privateRepo),
            CommitUserName,
            CommitUserEmail)
      _    <- git.push(newRepoName)
      shaO <- if (bootstrapTag.isDefined) git.lastCommitSha(newRepoName) else Try(None)
      _ <- if (bootstrapTag.isDefined) maybeCreateTag(newRepoName, shaO, BootstrapTagComment, bootstrapTag.get)
          else Try(Unit)
      _ <- if (bootstrapTag.isDefined) git.pushTags(newRepoName) else Try(Unit)
    } yield Unit
  }

  def maybeCreateTag(newRepoName: String, shaOpt: Option[String], tagText: String, version: String): Try[Unit] =
    shaOpt
      .map { sha =>
        git.tagAnnotatedCommit(newRepoName, sha, tagText, version)
      }
      .getOrElse {
        Failure(new IllegalAccessException("Didn't get a valid sha, check the list of commits"))
      }
}
