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

package uk.gov.hmrc.universalcreditliabilityapi.controllers

import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import play.api.mvc.Results.Forbidden
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.universalcreditliabilityapi.actions.AuthAction
import uk.gov.hmrc.universalcreditliabilityapi.connectors.HipConnector
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestData.*
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure as DwpFailure
import uk.gov.hmrc.universalcreditliabilityapi.services.{MappingService, SchemaValidationService}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.{CorrelationId, GovUkOriginatorId}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.{ErrorCodes, ForbiddenReason}

import scala.concurrent.{ExecutionContext, Future}

class UcLiabilityNotificationControllerSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val mat: Materializer    = app.materializer

  private val mockAuthAction              = mock[AuthAction]
  private val mockSchemaValidationService = mock[SchemaValidationService]
  private val mockMappingService          = mock[MappingService]
  private val mockHipConnector            = mock[HipConnector]

  private val controllerComponents: ControllerComponents = stubControllerComponents()

  private val testController = new UcLiabilityNotificationController(
    authAction = mockAuthAction,
    cc = controllerComponents,
    validationService = mockSchemaValidationService,
    mappingService = mockMappingService,
    hipConnector = mockHipConnector
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthAction, mockSchemaValidationService, mockMappingService, mockHipConnector)

    when(mockSchemaValidationService.validateOriginatorId(any()))
      .thenReturn(Right("SOME_GOVUK_ORIGINATOR_ID"))

    when(mockAuthAction.async(any[BodyParser[JsValue]])(any()))
      .thenAnswer { invocation =>
        val bodyParser = invocation.getArgument[BodyParser[JsValue]](0)
        val block      = invocation.getArgument[Request[JsValue] => Future[Result]](1)
        Helpers.stubControllerComponents().actionBuilder.async(bodyParser)(block)
      }
  }

  private def fakeRequest(
    json: JsValue,
    withOriginatorId: Boolean = true,
    withCorrelationId: Boolean = true
  ): FakeRequest[JsValue] = {
    val baseRequest = FakeRequest(POST, "/misc/universal-credit/liability/notification")
      .withBody(json)
      .withHeaders("Content-Type" -> "application/json")

    val withCorrelation =
      if (withCorrelationId) baseRequest.withHeaders(CorrelationId -> correlationId) else baseRequest
    if (withOriginatorId) withCorrelation.withHeaders(GovUkOriginatorId -> originatorId) else withCorrelation
  }

  "UcLiabilityNotificationController.submitLiabilityNotification()" must {
    "return 204 No Content" when {
      "HIP returns 204 for Insert request" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(eqTo(baseInsertDwpRequest)))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe NO_CONTENT
      }

      "HIP returns 204 for Terminate request" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseTerminateDwpRequest)))

        when(mockMappingService.mapRequest(eqTo(baseTerminateDwpRequest)))
          .thenReturn((nino, baseTerminateHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        val request = fakeRequest(terminateDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe NO_CONTENT
      }
    }

    "return 400 Bad Request" when {

      "validation service returns Left" in {
        val validationError = Results.BadRequest(
          Json.toJson(ApplicationConstants.invalidInputFailure("nationalInsuranceNumber"))
        )

        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Left(Future.successful(validationError)))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe BAD_REQUEST
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "code").as[String] mustBe ErrorCodes.InvalidInput
      }

      "validation fails with constraint violation (400.1)" in {
        val validationError = Results.BadRequest(
          Json.toJson(ApplicationConstants.invalidInputFailure("nationalInsuranceNumber"))
        )

        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any[Request[JsValue]]))
          .thenReturn(Left(Future.successful(validationError)))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] mustBe ErrorCodes.InvalidInput
      }
    }

    "return 403 Forbidden" when {
      "OriginatorId header is missing" in {
        when(mockSchemaValidationService.validateOriginatorId(any()))
          .thenReturn(
            Left(Future.successful(Forbidden(Json.toJson(ApplicationConstants.forbiddenFailure))))
          )

        val request = fakeRequest(insertDwpRequestJson(), withOriginatorId = false)
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe FORBIDDEN
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "code").as[String] mustBe ErrorCodes.ForbiddenCode
        (jsonResponse \ "message").as[String] mustBe ForbiddenReason
      }

      "HIP returns 403" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(any()))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(FORBIDDEN, "")))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe FORBIDDEN
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "code").as[String] mustBe ErrorCodes.ForbiddenCode
        (jsonResponse \ "message").as[String] mustBe ForbiddenReason
      }
    }

    "return 404 Not Found" when {
      "HIP returns 404" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(any()))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "return 422 Unprocessable Entity" when
      errorCodes.foreach { case (code, message) =>
        s"HIP returns 422 with code $code" in {
          val hipFailuresJson = Json.parse(
            s"""
               |{
               |  "failures": [
               |    {"code": "$code", "reason": "$message"}
               |  ]
               |}
               |""".stripMargin
          )

          val mappedResponse = DwpFailure(code = code, message = message)

          when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
            .thenReturn(Right((correlationId, baseInsertDwpRequest)))

          when(mockMappingService.mapRequest(any()))
            .thenReturn((nino, baseInsertHipRequest))

          when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
            .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, hipFailuresJson.toString)))

          when(mockMappingService.map422ResponseErrors(any()))
            .thenReturn(Some(mappedResponse))

          val request = fakeRequest(insertDwpRequestJson())
          val result  = testController.submitLiabilityNotification()(request)

          status(result) mustBe UNPROCESSABLE_ENTITY
          val jsonResponse = contentAsJson(result)
          (jsonResponse \ "code").as[String] mustBe code
          (jsonResponse \ "message").as[String] mustBe message
        }
      }

    "return 500 Internal Server Error" when {
      "HIP returns 400" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(any()))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "HIP returns 422 with no mapped errors" in {
        val hipFailuresJson = Json.parse(
          """
            |{
            |  "failures": [
            |    {"code": "UNKNOWN", "reason": "Unknown error"}
            |  ]
            |}
            |""".stripMargin
        )

        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(any()))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, hipFailuresJson.toString)))

        when(mockMappingService.map422ResponseErrors(any()))
          .thenReturn(None)

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "HIP returns unexpected status code" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(any()))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(IM_A_TEAPOT, "")))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

    "return 503 Service Unavailable" when {
      "HIP returns 503" in {
        when(mockSchemaValidationService.validateLiabilityNotificationRequest(any()))
          .thenReturn(Right((correlationId, baseInsertDwpRequest)))

        when(mockMappingService.mapRequest(any()))
          .thenReturn((nino, baseInsertHipRequest))

        when(mockHipConnector.sendUcLiability(any(), any(), any(), any())(using any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "")))

        val request = fakeRequest(insertDwpRequestJson())
        val result  = testController.submitLiabilityNotification()(request)

        status(result) mustBe SERVICE_UNAVAILABLE
        val jsonResponse = contentAsJson(result)
        (jsonResponse \ "code").as[String] mustBe "SERVER_ERROR"
        (jsonResponse \ "message")
          .as[String] mustBe "The 'misc/universal-credit/liability' API is currently unavailable"
      }
    }

  }

}
