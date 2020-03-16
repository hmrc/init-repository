/*
 * Copyright 2020 HM Revenue & Customs
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

import java.nio.file.Path

import uk.gov.hmrc.initrepository.Log

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

object Command {

  def run(cmd: String, inDir: Option[Path] = None): Try[List[String]] = {
    val cmdF = Future {
      val pb = inDir.fold(Process(cmd)) { path =>
        Process(cmd, cwd = path.toFile)
      }

      val out = ListBuffer[String]()
      val err = ListBuffer[String]()

      val logger   = ProcessLogger((s) => out.append(s), (e) => err.append(e))
      val exitCode = pb.!(logger)

      if (exitCode != 0) Log.info(s"got exit code $exitCode from command $cmd")
      if (err.size > 0) Log.debug(s"got following output on error stream from command $cmd \n  ${err.mkString("\n  ")}")

      Log.debug(s"output form '$cmd' is ${out.toList.mkString("\n")}")

      out.toList
    }

    Await.ready(cmdF, 2 minute)
    cmdF.value.get
  }

  def runArray(cmd: Array[String], inDir: Option[Path] = None): Try[List[String]] = {
    val cmdF = Future {
      val pb = inDir.fold(Process(cmd)) { path =>
        Process(cmd, cwd = path.toFile)
      }

      val out = ListBuffer[String]()
      val err = ListBuffer[String]()

      val logger   = ProcessLogger((s) => out.append(s), (e) => err.append(e))
      val exitCode = pb.!(logger)

      if (exitCode != 0) Log.info(s"got exit code $exitCode from command ${cmd.mkString(" ")}")
      if (err.size > 0)
        Log.error(s"got following output on error stream from command ${cmd.mkString(" ")} \n  ${err.mkString("\n  ")}")

      out.toList
    }

    Await.ready(cmdF, 2 minute)
    cmdF.value.get
  }
}
