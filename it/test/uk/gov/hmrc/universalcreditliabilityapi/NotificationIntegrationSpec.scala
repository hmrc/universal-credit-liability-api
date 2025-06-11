/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.universalcreditliabilityapi

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.universalcreditliabilityapi.support.WireMockIntegrationSpec
import play.api.libs.ws.writeableOf_JsValue

import java.util.UUID

class NotificationIntegrationSpec extends WireMockIntegrationSpec {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl  = s"http://localhost:$port"

  val correlationId: String = UUID.randomUUID().toString

  "POST /notification" must {
    "return 204 when HIP returns 204" in {
      val nino = ":nino"

      stubHipInsert(nino)

      val response = callPostNotification()

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(s"/person/$nino/liability/universal-credit")))
    }
  }

  def stubHipInsert(nino: String): StubMapping =
    stubFor(
      post(urlEqualTo("/person/:nino/liability/universal-credit"))
        .willReturn(
          aResponse()
            .withHeader("correlationId", correlationId)
            .withStatus(204)
        )
    )

  def callPostNotification(): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders("correlationId" -> correlationId)
      .post(Json.parse("{}"))
      .futureValue
}
