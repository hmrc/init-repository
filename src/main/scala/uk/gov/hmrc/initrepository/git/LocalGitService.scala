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

import scala.util.Try

class LocalGitService(git: LocalGitStore) {

  val CommitUserName = "hmrc-web-operations"
  val CommitUserEmail = "hmrc-web-operations@digital.hmrc.gov.uk"

  def buildReadmeTemplate(repoName:String, privateRepo: Boolean):String={

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



  def initialiseRepository(repoUrl: String, digitalServiceName: Option[String], privateRepo: Boolean): Try[Unit] = {

    def getManifestContents(digitalServiceName: Option[String]) = digitalServiceName.map(dsn => s"digital-service: $dsn")

    val newRepoName = repoUrl.split('/').last.stripSuffix(".git")
    for {
      _    <- git.cloneRepoURL(repoUrl)
      _    <- git.commitFileToRoot(newRepoName, ".gitignore", gitIgnoreContents, CommitUserName, CommitUserEmail)
      _    <- git.commitFileToRoot(newRepoName, "README.md", buildReadmeTemplate(newRepoName, privateRepo), CommitUserName, CommitUserEmail)
      _    <- git.commitFileToRoot(newRepoName, "repository.yaml", getManifestContents(digitalServiceName), CommitUserName, CommitUserEmail)
      _    <- git.push(newRepoName)
    } yield Unit
  }

}
