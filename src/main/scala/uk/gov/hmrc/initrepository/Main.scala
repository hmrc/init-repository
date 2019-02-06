/*
 * Copyright 2019 HM Revenue & Customs
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

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.initrepository.ArgParser.Config
import uk.gov.hmrc.initrepository.git.{LocalGitService, LocalGitStore}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]) {
    ArgParser.parser
      .parse(args, Config())
      .fold(throw new IllegalArgumentException("error while parsing provided arguments")) { config =>
        val root = LoggerFactory.getLogger(Log.loggerName).asInstanceOf[Logger]
        if (config.verbose) {
          root.setLevel(Level.DEBUG)
        } else {
          root.setLevel(Level.INFO)
        }

        val github = new Github {
          override val httpTransport: HttpTransport = new HttpTransport(config.githubUsername, config.githubToken)
          override val githubUrls: GithubUrls       = new GithubUrls()
        }

        val git = new LocalGitService(new LocalGitStore(Files.createTempDirectory("init-repository-git-store-")))

        try {
          val result = new Coordinator(github, git)
            .run(
              config.repository,
              config.teams,
              config.digitalServiceName,
              config.bootStrapTag,
              config.isPrivate,
              config.githubToken)

          Await.result(result, Duration(120, TimeUnit.SECONDS))
        } finally {
          github.close()
        }
      }
  }

}
