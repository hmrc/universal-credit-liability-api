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
import play.api.libs.ws.{WSClient, WSResponse, readableAsJson, writeableOf_String}
import uk.gov.hmrc.universalcreditliabilityapi.support.{MockAuthHelper, OpenApiValidator, WireMockIntegrationSpec}

import java.util.UUID
import scala.util.Random

class NotificationIntegrationSpec extends WireMockIntegrationSpec {

  private given WSClient       = app.injector.instanceOf[WSClient]
  private val openApiValidator = OpenApiValidator
    .fromResource("public/api/conf/1.0/application.yaml")
    .forPath(method = "POST", context = "/misc/universal-credit/liability", path = "/notification")

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

      val response = callPostNotification(requestBody)

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 204 when HIP returns 204 for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(nino)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.NO_CONTENT

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
    }

    "return 403 when originatorId header is missing from request" in {
      val nino = TestData.generateNino()

      val requestBody = TestData.validInsertionRequest(nino)

      val response = callPostNotification(
        requestBody,
        headers = TestData.validHeaders.removed("gov-uk-originator-id"),
        validateRequestAgainstOwnSchema = false
      )

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

      val response = callPostNotification(requestBody, validateRequestAgainstOwnSchema = false)

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

      val response = callPostNotification(
        requestBody,
        headers = TestData.validHeaders.removed("correlationId"),
        validateRequestAgainstOwnSchema = false
      )

      response.status mustBe Status.BAD_REQUEST

      verify(0, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
    }

    "return 500 when HIP returns 400 for insert" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validInsertionRequest(nino)

      stubHipInsert(nino, status = 400)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns 400 for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(nino, status = 400)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 403 when HIP returns 403 with code=403.2 and message=Forbidden for insert" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validInsertionRequest(nino)

      stubHipInsert(
        nino,
        status = 403,
        responseBody = """|{
                          |  "code": "any code",
                          |  "reason": "any insert forbidden reason"
                          |}
                          |""".stripMargin
      )

      val response = callPostNotification(requestBody)

      response.status mustBe Status.FORBIDDEN
      response.body[JsValue] mustBe Json.parse("""|{
                                                  |  "code": "403.2",
                                                  |  "message": "Forbidden"
                                                  |}
                                                  |""".stripMargin)

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 403 when HIP returns 403 with code=403.2 and message=Forbidden for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(
        nino,
        status = 403,
        responseBody = """|{
                          |  "code": "any code",
                          |  "reason": "any terminate forbidden reason"
                          |}
                          |""".stripMargin
      )

      val response = callPostNotification(requestBody)

      response.status mustBe Status.FORBIDDEN
      response.body[JsValue] mustBe Json.parse("""|{
                                                  |  "code": "403.2",
                                                  |  "message": "Forbidden"
                                                  |}
                                                  |""".stripMargin)

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 404 when HIP returns 404 for insert" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validInsertionRequest(nino)

      stubHipInsert(nino, status = 404)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.NOT_FOUND

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 404 when HIP returns 404 for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(nino, status = 404)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.NOT_FOUND

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 422 when HIP returns valid 422 for insert" in {
      val nino            = TestData.generateNino()
      val requestBody     = TestData.validInsertionRequest(nino)
      val hipResponseBody =
        """
          |{
          |  "failures": [
          |    {
          |      "reason": "Start date before 29/04/2013",
          |      "code": "65536"
          |    }
          |  ]
          |}
          |""".stripMargin

      stubHipInsert(nino, status = 422, responseBody = hipResponseBody)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.UNPROCESSABLE_ENTITY
      response.body[JsValue] mustBe Json.parse("""
                                                 |{
                                                 |  "code": "65536",
                                                 |  "message": "Start date before 29/04/2013"
                                                 |}
                                                 |""".stripMargin)

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 422 when HIP returns valid 422 for terminate" in {
      val nino            = TestData.generateNino()
      val requestBody     = TestData.validTerminateRequest(nino)
      val hipResponseBody =
        """
          |{
          |  "failures": [
          |    {
          |      "reason": "Start date before 29/04/2013",
          |      "code": "65536"
          |    }
          |  ]
          |}
          |""".stripMargin

      stubHipTermination(nino, status = 422, responseBody = hipResponseBody)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.UNPROCESSABLE_ENTITY
      response.body[JsValue] mustBe Json.parse("""
                                                 |{
                                                 |  "code": "65536",
                                                 |  "message": "Start date before 29/04/2013"
                                                 |}
                                                 |""".stripMargin)

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns invalid 422 for insert" in {
      val nino            = TestData.generateNino()
      val requestBody     = TestData.validInsertionRequest(nino)
      val hipResponseBody = """
                               |{
                               |  "failures2": [
                               |    {
                               |      "reasons": "Start date before 29/04/2013",
                               |      "codes": "65536"
                               |    }
                               |  ]
                               |}
                               |""".stripMargin

      stubHipInsert(nino, status = 422, responseBody = hipResponseBody)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns invalid 422 for terminate" in {
      val nino            = TestData.generateNino()
      val requestBody     = TestData.validTerminateRequest(nino)
      val hipResponseBody = "{}"

      stubHipTermination(nino, status = 422, responseBody = hipResponseBody)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns 422 with no specified cause for insert" in {
      val nino            = TestData.generateNino()
      val requestBody     = TestData.validInsertionRequest(nino)
      val hipResponseBody = """
                              |{
                              |  "failures": []
                              |}
                              |""".stripMargin

      stubHipInsert(nino, status = 422, responseBody = hipResponseBody)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns 422 with no specified cause for terminate" in {
      val nino            = TestData.generateNino()
      val requestBody     = TestData.validTerminateRequest(nino)
      val hipResponseBody = """
                              |{
                              |  "failures": []
                              |}
                              |""".stripMargin

      stubHipTermination(nino, status = 422, responseBody = hipResponseBody)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns 500 for insert" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validInsertionRequest(nino)

      stubHipInsert(nino, status = 500)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 500 when HIP returns 500 for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(nino, status = 500)

      val response = callPostNotification(requestBody)

      response.status mustBe Status.INTERNAL_SERVER_ERROR

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 503 when HIP returns 503 with a body matching the API platform's shutter response for insert" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validInsertionRequest(nino)

      stubHipInsert(nino, status = 503)

      val response = callPostNotification(requestBody, validateResponseAgainstOwnSchema = false)

      response.status mustBe Status.SERVICE_UNAVAILABLE
      response.body[JsValue] mustBe Json.parse("""
          |{
          |   "code": "SERVER_ERROR",
          |   "message": "The 'misc/universal-credit/liability' API is currently unavailable"
          |}
          |""".stripMargin)

      verify(1, postRequestedFor(urlEqualTo(hipInsertionUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "return 503 when HIP returns 503 with a body matching the API platform's shutter response for terminate" in {
      val nino        = TestData.generateNino()
      val requestBody = TestData.validTerminateRequest(nino)

      stubHipTermination(nino, status = 503)

      val response = callPostNotification(requestBody, validateResponseAgainstOwnSchema = false)

      response.status mustBe Status.SERVICE_UNAVAILABLE
      response.body[JsValue] mustBe Json.parse("""
                                                 |{
                                                 |   "code": "SERVER_ERROR",
                                                 |   "message": "The 'misc/universal-credit/liability' API is currently unavailable"
                                                 |}
                                                 |""".stripMargin)

      verify(1, postRequestedFor(urlEqualTo(hipTerminationUrl(nino))))
      MockAuthHelper.verifyAuthWasCalled()
    }

    "verify that liabilityEndDate is not sent to HIP if it sent as part of the insert request" in {
      val nino = TestData.generateNino()

      val requestBody = Json.parse(s"""
           |{
           |  "nationalInsuranceNumber": "$nino",
           |  "universalCreditRecordType": "UC",
           |  "universalCreditAction": "Insert",
           |  "dateOfBirth": "2002-10-10",
           |  "liabilityEndDate": "2015-08-19",
           |  "liabilityStartDate": "2025-08-19"
           |}
           |""".stripMargin)

      val hipRequestBody = s"""
                              |{
                              |  "universalCreditLiabilityDetails": {
                              |    "universalCreditRecordType": "UC",
                              |    "dateOfBirth": "2002-10-10",
                              |    "liabilityStartDate": "2025-08-19"
                              |  }
                              |}
                              |""".stripMargin

      stubHipInsert(nino, expectedRequestBody = Some(hipRequestBody))

      val response = callPostNotification(requestBody, validateRequestAgainstOwnSchema = false)

      response.status mustBe Status.NO_CONTENT

      verify(
        1,
        postRequestedFor(urlEqualTo(hipInsertionUrl(nino)))
      )
      MockAuthHelper.verifyAuthWasCalled()
    }

    "verify correct request body is sent to HIP for terminate" in {
      val nino = TestData.generateNino()

      val requestBody = Json.parse(s"""
           |{
           |  "nationalInsuranceNumber": "$nino",
           |  "universalCreditRecordType": "UC",
           |  "universalCreditAction": "Terminate",
           |  "liabilityStartDate": "2025-08-19",
           |  "liabilityEndDate": "2025-08-20"
           |}
           |""".stripMargin)

      val hipRequestBody =
        s"""
           |{
           |  "ucLiabilityTerminationDetails": {
           |    "universalCreditRecordType": "UC",
           |    "liabilityStartDate": "2025-08-19",
           |    "liabilityEndDate": "2025-08-20"
           |  }
           |}
           |""".stripMargin

      stubHipTermination(nino, expectedRequestBody = Some(hipRequestBody))

      val response = callPostNotification(requestBody, validateRequestAgainstOwnSchema = false)

      response.status mustBe Status.NO_CONTENT

      verify(
        1,
        postRequestedFor(urlEqualTo(hipTerminationUrl(nino)))
      )
      MockAuthHelper.verifyAuthWasCalled()
    }
  }

  def stubHipInsert(
    nino: String,
    status: Int = 204,
    responseBody: String = "",
    expectedRequestBody: Option[String] = None
  ): StubMapping = {

    val mappingBuilder = post(urlEqualTo(s"/person/$nino/liability/universal-credit"))

    val mappingWithBody = expectedRequestBody match {
      case Some(body) => mappingBuilder.withRequestBody(equalToJson(body))
      case None       => mappingBuilder
    }

    stubFor(
      mappingWithBody.willReturn(
        aResponse()
          .withHeader("content-type", "application/json")
          .withHeader("correlationId", TestData.correlationId)
          .withStatus(status)
          .withBody(responseBody)
      )
    )
  }

  def stubHipTermination(
    nino: String,
    status: Int = 204,
    responseBody: String = "",
    expectedRequestBody: Option[String] = None
  ): StubMapping = {

    val mappingBuilder = post(urlEqualTo(s"/person/$nino/liability/universal-credit/termination"))

    val mappingWithBody = expectedRequestBody match {
      case Some(body) => mappingBuilder.withRequestBody(equalToJson(body))
      case None       => mappingBuilder
    }

    stubFor(
      mappingWithBody.willReturn(
        aResponse()
          .withHeader("content-type", "application/json")
          .withHeader("correlationId", TestData.correlationId)
          .withStatus(status)
          .withBody(responseBody)
      )
    )
  }

  def callPostNotification(
    body: String | JsValue,
    headers: Map[String, String] = TestData.validHeaders,
    validateRequestAgainstOwnSchema: Boolean = true,
    validateResponseAgainstOwnSchema: Boolean = true
  ): WSResponse = {

    val request = openApiValidator
      .newRequestBuilder()
      .withHttpHeaders(headers.toSeq: _*)
      .addHttpHeaders("content-type" -> "application/json")
      .withMethod("POST")
      .withBody(body match {
        case j: JsValue => Json.stringify(j)
        case s: String  => s
      })

    if (validateRequestAgainstOwnSchema) {
      val requestValidationErrors = openApiValidator.validateRequest(request)
      requestValidationErrors mustBe List.empty
    }

    val response = request.execute().futureValue

    if (validateResponseAgainstOwnSchema) {
      val responseValidationErrors = openApiValidator.validateResponse(response)
      responseValidationErrors mustBe List.empty
    }

    response
  }

}

object TestData {
  val testOriginatorId                  = "testId"
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
       |  "liabilityStartDate": "2025-08-19"
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
