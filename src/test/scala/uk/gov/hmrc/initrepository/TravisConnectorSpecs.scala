/*
 * Copyright 2016 HM Revenue & Customs
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

import java.net.URL

import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.initrepository.wiremock.{TravisWireMocks, WireMockEndpoints}

class TravisConnectorSpecs extends WordSpec with Matchers with FutureValues with WireMockEndpoints with TravisWireMocks {

  val gitHubKey = "key"
  val accessToken = "access"

  val urls = new TravisUrls("http://localhost:6001")
  val transport = new HttpTransport {
    override def creds: ServiceCredentials = ServiceCredentials("username", s"$gitHubKey")
  }

  val travisConnector = new TravisConnector {
    override def httpTransport: HttpTransport = transport
    override def travisUrls: TravisUrls = urls
  }

  "Travis Connector" should {

    "Return an access token given a valid github api key" in {

      givenTravisExpects(
        method = GET,
        url = new URL(urls.githubAuthentication.toString),
        payload = Some(s"""{"github_token":"$gitHubKey"}"""),
        willRespondWith = (200, Some("""{"access_token":"xxxxxxx"}""")))

      val result = travisConnector.authenticate.await
      result.accessToken should be("xxxxxxx")

    }

    "Throw a requestexception if authentication receives a status other than 200" in {

      givenTravisExpects(
        method = GET,
        url = new URL(urls.githubAuthentication.toString),
        payload = Some(s"""{"github_token":"$gitHubKey"}"""),
        willRespondWith = (500, None))

      a [RequestException] should be thrownBy travisConnector.authenticate.await

    }

    "Return Unit on a succesful sync" in {

      givenTravisExpects(
        method = POST,
        url = new URL(urls.syncWithGithub.toString),
        extraHeaders = Map("Authorization" -> s"token $accessToken"),
        willRespondWith = (200, None))

      travisConnector.syncWithGithub(accessToken).await

    }

    "Throw a requestexception if sync fails" in {

      givenTravisExpects(
        method = POST,
        url = new URL(urls.syncWithGithub.toString),
        extraHeaders = Map("Authorization" -> s"token $accessToken"),
        willRespondWith = (500, None))

      a [RequestException] should be thrownBy travisConnector.syncWithGithub(accessToken).await

    }

  }

}
