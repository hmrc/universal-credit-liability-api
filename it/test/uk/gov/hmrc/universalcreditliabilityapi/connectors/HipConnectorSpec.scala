/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.universalcreditliabilityapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.*
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType.LCW_LCWRA
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, TerminateLiabilityRequest, UcLiabilityTerminationDetails, UniversalCreditLiabilityDetails}
import uk.gov.hmrc.universalcreditliabilityapi.support.WireMockIntegrationSpec

import java.util.Base64

class HipConnectorSpec extends WireMockIntegrationSpec {

  private val testNino: String          = "AA123456"
  private val testCorrelationId: String = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"
  private val testOriginatorId: String  = "testOriginatorId"

  private lazy val hipConnector: HipConnector = app.injector.instanceOf[HipConnector]

  private given HeaderCarrier = HeaderCarrier()

  private def hipInsertionUrl(nino: String)   = s"/person/$nino/liability/universal-credit"
  private def hipTerminationUrl(nino: String) = s"/person/$nino/liability/universal-credit/termination"

  private def expectedBasicAuth: String = {
    val credentials = s"$testHipClientId:$testHipClientSecret"
    s"Basic ${Base64.getEncoder.encodeToString(credentials.getBytes("UTF-8"))}"
  }

  private val insertPayload: InsertLiabilityRequest = InsertLiabilityRequest(
    universalCreditLiabilityDetails = UniversalCreditLiabilityDetails(
      universalCreditRecordType = LCW_LCWRA,
      dateOfBirth = "2002-10-10",
      liabilityStartDate = "2015-08-19"
    )
  )

  private val terminatePayload: TerminateLiabilityRequest = TerminateLiabilityRequest(
    ucLiabilityTerminationDetails = UcLiabilityTerminationDetails(
      universalCreditRecordType = LCW_LCWRA,
      liabilityStartDate = "2015-08-19",
      liabilityEndDate = "2025-01-04"
    )
  )

  private def stubHipResponseFor(url: String, status: Int)(implicit responseBody: JsObject): StubMapping =
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(responseBody.toString())
        )
    )

  private def verifyHipHeaders(url: String, expectedBody: Option[String] = None): Unit = {
    val requestBuilder = postRequestedFor(urlEqualTo(url))
      .withHeader("Authorization", equalTo(expectedBasicAuth))
      .withHeader("correlationId", equalTo(testCorrelationId))
      .withHeader("gov-uk-originator-id", equalTo(testOriginatorId))

    val withBody = expectedBody.fold(requestBuilder)(body => requestBuilder.withRequestBody(equalToJson(body)))

    verify(withBody)
  }

  "HipConnector.sendUcLiability" must {

    "send an 'Insert' request with all required HIP headers and return 204" in {
      implicit val responseBody: JsObject = Json.obj()

      stubHipResponseFor(hipInsertionUrl(testNino), NO_CONTENT)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe NO_CONTENT

      verifyHipHeaders(
        hipInsertionUrl(testNino),
        Some(Json.toJson(insertPayload).toString())
      )
    }

    "send a 'Terminate' request with all required HIP headers and return 204" in {
      implicit val responseBody: JsObject = Json.obj()

      stubHipResponseFor(hipTerminationUrl(testNino), NO_CONTENT)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = terminatePayload
        )
        .futureValue

      result.status mustBe NO_CONTENT

      verifyHipHeaders(
        hipTerminationUrl(testNino),
        Some(Json.toJson(terminatePayload).toString())
      )
    }

    "handle BAD_REQUEST (400) response" in {
      implicit val responseBody: JsObject = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code"   -> "INVALID_PAYLOAD",
            "reason" -> "Submission has not passed validation. Invalid payload."
          )
        )
      )

      stubHipResponseFor(hipInsertionUrl(testNino), BAD_REQUEST)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe BAD_REQUEST
      result.json mustBe responseBody
    }

    "handle FORBIDDEN (403) response" in {
      implicit val responseBody: JsObject = Json.obj(
        "code"   -> "FORBIDDEN",
        "reason" -> "Forbidden"
      )

      stubHipResponseFor(hipInsertionUrl(testNino), FORBIDDEN)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe FORBIDDEN
      result.json mustBe responseBody
    }

    "handle NOT_FOUND (404) response" in {
      implicit val responseBody: JsObject = Json.obj(
        "code"   -> "NOT_FOUND",
        "reason" -> "Resource not found"
      )

      stubHipResponseFor(hipInsertionUrl(testNino), NOT_FOUND)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe NOT_FOUND
      result.json mustBe responseBody
    }

    "handle UNPROCESSABLE_ENTITY (422) response" in {
      implicit val responseBody: JsObject = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code"   -> "65536",
            "reason" -> "Start date before 29/04/2013"
          )
        )
      )

      stubHipResponseFor(hipInsertionUrl(testNino), UNPROCESSABLE_ENTITY)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe UNPROCESSABLE_ENTITY
      result.json mustBe responseBody
    }

    "handle INTERNAL_SERVER_ERROR (500) response" in {
      implicit val responseBody: JsObject = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code"   -> "SERVER_ERROR",
            "reason" -> "Internal Server Error"
          )
        )
      )

      stubHipResponseFor(hipInsertionUrl(testNino), INTERNAL_SERVER_ERROR)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe responseBody
    }

    "handle SERVICE_UNAVAILABLE (503) response" in {
      implicit val responseBody: JsObject = Json.obj(
        "failures" -> Json.arr(
          Json.obj(
            "code"   -> "SERVICE_UNAVAILABLE",
            "reason" -> "Dependent systems are currently not responding"
          )
        )
      )

      stubHipResponseFor(hipInsertionUrl(testNino), SERVICE_UNAVAILABLE)

      val result = hipConnector
        .sendUcLiability(
          nationalInsuranceNumber = testNino,
          correlationId = testCorrelationId,
          originatorId = testOriginatorId,
          requestObject = insertPayload
        )
        .futureValue

      result.status mustBe SERVICE_UNAVAILABLE
      result.json mustBe responseBody
    }
  }
}
