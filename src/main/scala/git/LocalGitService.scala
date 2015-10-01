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

import scala.concurrent.Future
import uk.gov.hmrc.initrepository.ImplicitPimps._
import scala.concurrent.ExecutionContext.Implicits.global

class LocalGitService(git: LocalGitStore) {

  val BootstrapTagComment = "Bootstrap tag"
  val BootstrapTagVersion = "v0.1.0"

  def cloneAndTag(repoUrl: String) : Future[Unit]={
    val newRepoName = repoUrl.split('/').last.stripSuffix(".git")
    for(
      _ <- git.cloneRepoURL(repoUrl).await;
      //sha <- git.commitFileToRoot(newRepoName, "README.MD", "Put useful info here").await;
      shaO <- git.lastCommitSha(newRepoName);
      _ <- maybeCreateTag(newRepoName, shaO, BootstrapTagComment, BootstrapTagVersion).await;
      _ <- git.push(newRepoName).await;
      _ <- git.pushTags(newRepoName)
    ) yield Unit
  }

  def maybeCreateTag(newRepoName:String, shaOpt:Option[String], tagText:String, version:String):Future[Unit]={
    shaOpt.map{ sha =>
      git.tagAnnotatedCommit(newRepoName, sha, tagText, version)
    }.getOrElse{
      Future.failed(new IllegalAccessException("Didn't get a valid sha, check the list of commits"))
    }
  }
}
