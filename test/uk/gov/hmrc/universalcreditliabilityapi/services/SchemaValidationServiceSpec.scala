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

package uk.gov.hmrc.universalcreditliabilityapi.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers.*
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestData.*
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestHelpers.{buildFakeRequest, extractLeftOrFail, jsObjectWith, jsObjectWithout}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.{InsertUcLiabilityRequest, TerminateUcLiabilityRequest}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.{CorrelationId, OriginatorId}

import scala.concurrent.Future

class SchemaValidationServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {

  val testSchemaValidationService = new SchemaValidationService()

  private def assertBadRequest(result: Either[Future[Result], _], expectedField: String): Unit =
    whenReady(extractLeftOrFail(result)) { actualResult =>
      actualResult.header.status mustBe BAD_REQUEST

      val body = contentAsJson(Future.successful(actualResult))
      (body \ "code").as[String] mustBe ApplicationConstants.ErrorCodes.InvalidInput
      (body \ "message").as[String] mustBe ApplicationConstants.invalidInputFailure(expectedField).message
    }

  private def assertForbidden(result: Either[Future[Result], _]): Unit =
    whenReady(extractLeftOrFail(result)) { actualResult =>
      actualResult.header.status mustBe FORBIDDEN

      val body = contentAsJson(Future.successful(actualResult))
      (body \ "code").as[String] mustBe ApplicationConstants.ErrorCodes.ForbiddenCode
      (body \ "message").as[String] mustBe ApplicationConstants.forbiddenFailure.message
    }

  "SchemaValidationServiceSpec.validateOriginatorId" must {
    "return Right" when {
      "originatorId is present and valid" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = OriginatorId -> originatorId)
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result mustBe Right(originatorId)
      }

      "originatorId has 3 valid characters" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = OriginatorId -> "ABC")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result match {
          case Right(value) => assert(value == "ABC")
          case Left(_)      => fail("Expected Right but got Left")
        }
      }

      "originatorId has 40 valid characters" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = OriginatorId -> "A" * 40)
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result match {
          case Right(value) => assert(value == "A" * 40)
          case Left(_)      => fail("Expected Right but got Left")
        }
      }
    }

    "return Left (403 Forbidden)" when {
      "originatorId header is missing" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json)
        val result  = testSchemaValidationService.validateOriginatorId(request)

        assertForbidden(result)
      }

      "originatorId is shorter than 3 characters" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = OriginatorId -> "AB")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        assertForbidden(result)
      }

      "originatorId is longer than 40 characters" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = OriginatorId -> "A" * 41)
        val result  = testSchemaValidationService.validateOriginatorId(request)

        assertForbidden(result)
      }
    }
  }

  "SchemaValidationServiceSpec.validateLiabilityNotificationRequest" must {

    "return Right" when {

      for (recordType <- validRecordTypes) {
        s"given a valid 'Insert' Request of '$recordType' record type" in {
          val json    = insertDwpRequestJson(recordType)
          val request = buildFakeRequest(json, CorrelationId -> correlationId)
          val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          result mustBe Right((correlationId, json.as[InsertUcLiabilityRequest]))
        }

        s"given a valid 'Terminate' Request of '$recordType' record type" in {
          val json    = terminateDwpRequestJson(recordType)
          val request = buildFakeRequest(json, CorrelationId -> correlationId)
          val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          result mustBe Right((correlationId, json.as[TerminateUcLiabilityRequest]))
        }
      }

    }

    "return Left (400 Bad Request)" when {

      for {
        recordType <- validRecordTypes
        field      <- requiredInsertDwpFields
      }
        s"given an 'Insert' Request of '$recordType' missing $field" in {
          val invalidJson = jsObjectWithout(insertDwpRequestJson(recordType), field)
          val request     = buildFakeRequest(invalidJson, CorrelationId -> correlationId)
          val result      = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          assertBadRequest(result, field)
        }

      for {
        recordType <- validRecordTypes
        field      <- requiredTerminateDwpFields
      }
        s"given a 'Terminate' Request of '$recordType' missing $field" in {
          val invalidJson = jsObjectWithout(terminateDwpRequestJson(recordType), field)
          val request     = buildFakeRequest(invalidJson, CorrelationId -> correlationId)
          val result      = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          assertBadRequest(result, field)
        }

      for ((field, invalidValue) <- invalidInsertDwpRequestValues)
        s"given an 'Insert' Request with an invalid '$field' field" in {
          val invalidJson = jsObjectWith(insertDwpRequestJson(), field, Json.toJson(invalidValue))
          val request     = buildFakeRequest(invalidJson, CorrelationId -> correlationId)
          val result      = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          assertBadRequest(result, field)
        }

      for ((field, invalidValue) <- invalidTerminateDwpRequestValues)
        s"given a 'Terminate' Request with an invalid '$field' field" in {
          val invalidJson = jsObjectWith(terminateDwpRequestJson(), field, Json.toJson(invalidValue))
          val request     = buildFakeRequest(invalidJson, CorrelationId -> correlationId)
          val result      = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          assertBadRequest(result, field)
        }

      "given an 'Insert' Request with correlationId missing from the headers" in {
        val request = buildFakeRequest(insertDwpRequestJson())
        val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

        assertBadRequest(result, "correlationId")
      }

      "given a 'Terminate' Request with correlationId missing rom the headers" in {
        val request = buildFakeRequest(terminateDwpRequestJson())
        val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

        assertBadRequest(result, "correlationId")
      }

      "given an 'Insert' Request with invalid correlationId" in {
        val request = buildFakeRequest(insertDwpRequestJson(), CorrelationId -> "INVALID_CORRELATION_ID")
        val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

        assertBadRequest(result, "correlationId")
      }

      "given a 'Terminate' Request with invalid correlationId" in {
        val request = buildFakeRequest(terminateDwpRequestJson(), CorrelationId -> "INVALID_CORRELATION_ID")
        val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

        assertBadRequest(result, "correlationId")
      }

      "payload is empty" in {
        val request = buildFakeRequest(Json.obj(), CorrelationId -> correlationId)
        val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

        assertBadRequest(result, "universalCreditAction")
      }

    }
  }

}
