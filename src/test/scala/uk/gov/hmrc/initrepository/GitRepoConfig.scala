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

package uk.gov.hmrc.initrepository

import uk.gov.hmrc.initrepository.git.LocalGitStore


object GitRepoConfig {
  //to satisfy git on travis while running the tests
  def withNameConfig[T](store : LocalGitStore, reponame: String)(f : => T ) : T = {

    val result = f

    store.gitCommandParts(Array("config", "user.email", "'test@example.com'"), inRepo = Some(reponame)).map { _ => Unit }
    store.gitCommandParts(Array("config", "user.name", "'testUser'"), inRepo = Some(reponame)).map { _ => Unit }

    result
  }
}
