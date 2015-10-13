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

package uk.gov.hmrc.initrepository.bintray

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BintrayService {

  def bintray:Bintray
  val repositories:Set[String]


  def createPackagesFor(newPackageName:String):Future[Unit]={
    Future.sequence {
      repositories.map { r =>
        bintray.createPackage(r, newPackageName)
      }
    }.map(_.head)
  }

  def reposContainingPackage(newPackageName:String):Future[Set[String]]={
    Future.sequence {
      repositories.map { r =>
        bintray.containsPackage(r, newPackageName).map { resp => r -> resp}
      }
    }.map { repoResponses =>
      repoResponses.filter(_._2).map(_._1)
    }
  }

  def close() = bintray.close()
}
