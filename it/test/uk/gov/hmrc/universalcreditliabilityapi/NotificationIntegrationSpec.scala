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
import play.api.libs.ws.{WSClient, WSResponse, writeableOf_JsValue}
import uk.gov.hmrc.universalcreditliabilityapi.support.WireMockIntegrationSpec

import java.util.UUID

class NotificationIntegrationSpec extends WireMockIntegrationSpec {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl  = s"http://localhost:$port"
  private val nino     = "AA000001"

  "POST /notification" must {
    "return 204 when HIP returns 204 for insert" in {
      stubHipInsert(nino)

      val response = callPostInsertion(nino)

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(s"/person/$nino/liability/universal-credit")))
    }

    "return 204 when HIP returns 204 for terminate" in {
      stubHipTermination(nino)

      val response = callPostTermination(nino)

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(s"/person/$nino/liability/universal-credit/termination")))
    }

    "return 403 when originatorId header is missing from request" in {

      val response = callPostInsertionWithoutOriginatorId(nino)

      response.status mustBe Status.FORBIDDEN

      verify(0, postRequestedFor(urlEqualTo(s"/person/$nino/liability/universal-credit")))
    }

    "return 400 when invalid universal credit action is passed in the request" in {

      val response = callPostInsertionWithInvalidUniversalCreditAction(nino)

      response.status mustBe Status.BAD_REQUEST

      verify(0, postRequestedFor(urlEqualTo(s"/person/$nino/liability/universal-credit/termination")))
    }

    "return 400 when more than one request field is invalid" in {

      val response = callPostInsertionWithInvalidRequestBody(nino)

      response.status mustBe Status.BAD_REQUEST

      verify(0, postRequestedFor(urlEqualTo(s"/person/$nino/liability/universal-credit/termination")))
    }
  }

  def stubHipInsert(nino: String): StubMapping =
    stubFor(
      post(urlEqualTo(s"/person/$nino/liability/universal-credit"))
        .willReturn(
          aResponse()
            .withHeader("content-type", "application/json")
            .withHeader("correlationId", TestData.correlationId)
            .withHeader("gov-uk-originator-id", TestData.testOriginatorId)
            .withBody(Json.parse("{}").toString())
            .withStatus(204)
        )
    )

  def stubHipTermination(nino: String): StubMapping =
    stubFor(
      post(urlEqualTo(s"/person/$nino/liability/universal-credit/termination"))
        .willReturn(
          aResponse()
            .withHeader("correlationId", TestData.correlationId)
            .withHeader("gov-uk-originator-id", TestData.testOriginatorId)
            .withStatus(204)
        )
    )

  def callPostInsertion(nino: String): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(
        "correlationId"        -> TestData.correlationId,
        "gov-uk-originator-id" -> TestData.testOriginatorId
      )
      .post(Json.parse(s"""
          |{
          |  "nationalInsuranceNumber": "$nino",
          |  "universalCreditRecordType": "UC",
          |  "universalCreditAction": "Insert",
          |  "dateOfBirth": "2002-10-10",
          |  "liabilityStartDate": "2025-08-19"
          |}
          |""".stripMargin))
      .futureValue

  def callPostTermination(nino: String): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(
        "correlationId"        -> TestData.correlationId,
        "gov-uk-originator-id" -> TestData.testOriginatorId
      )
      .post(Json.parse(s"""
           |{
           |  "nationalInsuranceNumber": "$nino",
           |  "universalCreditRecordType": "LCW/LCWRA",
           |  "universalCreditAction": "Terminate",
           |  "liabilityStartDate": "2025-08-19",
           |  "liabilityEndDate": "2025-08-19"
           |}
           |""".stripMargin))
      .futureValue

  def callPostInsertionWithoutOriginatorId(nino: String): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(
        "correlationId" -> TestData.correlationId
      )
      .post(Json.parse(s"""
           |{
           |  "nationalInsuranceNumber": "$nino",
           |  "universalCreditRecordType": "UC",
           |  "universalCreditAction": "Insert",
           |  "dateOfBirth": "2002-10-10",
           |  "liabilityStartDate": "2025-08-19"
           |}
           |""".stripMargin))
      .futureValue

  def callPostInsertionWithInvalidUniversalCreditAction(nino: String): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(
        "correlationId"        -> TestData.correlationId,
        "gov-uk-originator-id" -> TestData.testOriginatorId
      )
      .post(Json.parse(s"""
           |{
           |  "nationalInsuranceNumber": "$nino",
           |  "universalCreditRecordType": "UC",
           |  "universalCreditAction": "Insrt",
           |  "dateOfBirth": "2002-10-10",
           |  "liabilityStartDate": "2025-08-19"
           |}
           |""".stripMargin))
      .futureValue

  def callPostInsertionWithInvalidRequestBody(nino: String): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(
        "gov-uk-originator-id" -> TestData.testOriginatorId
      )
      .post(Json.parse(s"""
             |{
             |  "nationalInsuranceNumber": "$nino",
             |  "universalCreditRecordType": "UCW",
             |  "universalCreditAction": "Insert",
             |  "dateOfBirth": "2002-10-10",
             |  "liabilityStartDate": "2025-14-19"
             |}
             |""".stripMargin))
      .futureValue
}

object TestData {
  val testOriginatorId      = ""
  val correlationId: String = UUID.randomUUID().toString
}
