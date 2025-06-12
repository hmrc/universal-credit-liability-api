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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse, writeableOf_String}
import uk.gov.hmrc.universalcreditliabilityapi.support.{MockAuthHelper, WireMockIntegrationSpec}

import java.util.UUID
import scala.util.Random

class NotificationIntegrationSpec extends WireMockIntegrationSpec {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl  = s"http://localhost:$port"

  override def beforeEach(): Unit = {
    super.beforeEach()
    MockAuthHelper.mockAuthOk()
  }

  def hipInsertionUrl(nino: String)   = s"/person/$nino/liability/universal-credit"
  def hipTerminationUrl(nino: String) = s"/person/$nino/liability/universal-credit/termination"

  "POST /notification" must {
    "return 204 when HIP returns 204 for insert" in {
      val nino        = TestData.generateNino()
      val requestBody = Json.parse(s"""
                                   |{
                                   |  "nationalInsuranceNumber": "$nino",
                                   |  "universalCreditRecordType": "UC",
                                   |  "universalCreditAction": "Insert",
                                   |  "dateOfBirth": "2002-10-10",
                                   |  "liabilityStartDate": "2025-08-19"
                                   |}
                                   |""".stripMargin)

      stubHipInsert(nino)

      val response = callPostInsertion(requestBody)

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 204 when HIP returns 204 for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(nino)

      val response = callPostTermination(requestBody)

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
    }

    "return 403 when originatorId header is missing from request" in {
      val nino = TestData.generateNino()

      val requestBody = TestData.validInsertionRequest(nino)

      val response = callPostInsertion(requestBody, headers = TestData.validHeaders.removed("gov-uk-originator-id"))

      response.status mustBe Status.FORBIDDEN

      verify(0, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
    }

    "return 400 when invalid universal credit action is passed in the request" in {
      val nino = TestData.generateNino()

      val requestBody = Json.parse(s"""
                                      |{
                                      |  "nationalInsuranceNumber": "$nino",
                                      |  "universalCreditRecordType": "UC",
                                      |  "universalCreditAction": "Insrt",
                                      |  "dateOfBirth": "2002-10-10",
                                      |  "liabilityStartDate": "2025-08-19"
                                      |}
                                      |""".stripMargin)

      val response = callPostInsertion(requestBody)

      response.status mustBe Status.BAD_REQUEST

      verify(0, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
    }

    "return 400 when more than one request field is invalid" in {
      val nino = TestData.generateNino()

      val requestBody = Json.parse(s"""
                                      |{
                                      |  "nationalInsuranceNumber": "$nino",
                                      |  "universalCreditRecordType": "UCW",
                                      |  "universalCreditAction": "Insert",
                                      |  "dateOfBirth": "2002-10-10",
                                      |  "liabilityStartDate": "2025-14-19"
                                      |}
                                      |""".stripMargin)

      val response = callPostInsertion(requestBody, headers = TestData.validHeaders.removed("correlationId"))

      response.status mustBe Status.BAD_REQUEST

      verify(0, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
    }
  }

  def stubHipInsert(nino: String, status: Int = 204, body: String = ""): StubMapping =
    stubFor(
      post(urlEqualTo(s"/person/$nino/liability/universal-credit"))
        .willReturn(
          aResponse()
            .withHeader("content-type", "application/json")
            .withHeader("correlationId", TestData.correlationId)
            .withBody(body)
            .withStatus(status)
        )
    )

  def stubHipTermination(nino: String, status: Int = 204, body: String = ""): StubMapping =
    stubFor(
      post(urlEqualTo(s"/person/$nino/liability/universal-credit/termination"))
        .willReturn(
          aResponse()
            .withHeader("content-type", "application/json")
            .withHeader("correlationId", TestData.correlationId)
            .withBody(body)
            .withStatus(status)
        )
    )

  def callPostInsertion(body: String | JsValue, headers: Map[String, String] = TestData.validHeaders): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(headers.toSeq: _*)
      .addHttpHeaders("content-type" -> "application/json")
      .post(body match {
        case j: JsValue => Json.stringify(j)
        case s: String  => s
      })
      .futureValue

  def callPostTermination(body: String | JsValue, headers: Map[String, String] = TestData.validHeaders): WSResponse =
    wsClient
      .url(s"$baseUrl/notification")
      .withHttpHeaders(headers.toSeq: _*)
      .addHttpHeaders("content-type" -> "application/json")
      .post(body match {
        case j: JsValue => Json.stringify(j)
        case s: String  => s
      })
      .futureValue

}

object TestData {
  val testOriginatorId                  = ""
  val correlationId: String             = UUID.randomUUID().toString
  val validHeaders: Map[String, String] = Map(
    "authorization"        -> "",
    "correlationId"        -> correlationId,
    "gov-uk-originator-id" -> testOriginatorId
  )

  def generateNino(): String = {
    val number = f"${Random.nextInt(100000)}%06d"
    val nino   = s"AA$number"
    nino
  }

  def validInsertionRequest(nino: String): JsValue = Json.parse(s"""
       |{
       |  "nationalInsuranceNumber": "$nino",
       |  "universalCreditRecordType": "LCW/LCWRA",
       |  "universalCreditAction": "Insert",
       |  "dateOfBirth": "2002-10-10",
       |  "liabilityEndDate": "2025-08-19"
       |}
       |""".stripMargin)

  def validTerminateRequest(nino: String): JsValue = Json.parse(s"""
       |{
       |  "nationalInsuranceNumber": "$nino",
       |  "universalCreditRecordType": "LCW/LCWRA",
       |  "universalCreditAction": "Terminate",
       |  "liabilityStartDate": "2025-08-19",
       |  "liabilityEndDate": "2025-08-19"
       |}
       |""".stripMargin)
}
