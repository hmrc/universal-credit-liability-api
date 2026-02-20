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
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.{InsertUniversalCreditLiability, TerminateUniversalCreditLiability}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.{CorrelationId, GovUkOriginatorId}

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
      "GovUkOriginatorId is present and valid" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> govUkOriginatorId)
        val result  = testSchemaValidationService.validateGovUkOriginatorId(request)

        result mustBe Right(govUkOriginatorId)
      }
    }

    "return right" when {
      "given a valid originatorId" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> ("A" * 3))
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result mustBe Right("A" * 3)
      }

      "given a valid originatorId for Special characters: '{}, [], (), @, !, *, -, ?'" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "{[(V@l!d-0r!g!n4t*r-1D?)]}")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result mustBe Right("{[(V@l!d-0r!g!n4t*r-1D?)]}")
      }

      "given a valid originatorId for Special characters: '//, \\, €, £, $, !, *, -, &, +, ?' " in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "//:€anc£l_Orignin&t+r_1D$;\\")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result mustBe Right("//:€anc£l_Orignin&t+r_1D$;\\")
      }

      "given a valid originatorId for Special characters: '<>, `, #, ',' ~, ^, ., %,' " in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "<`#,Va|1d~0rig^nator=ID.%`>")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        result mustBe Right("<`#,Va|1d~0rig^nator=ID.%`>")
      }
    }

    "return Left (403 Forbidden)" when {
      "GovUkOriginatorId header is missing" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json)
        val result  = testSchemaValidationService.validateGovUkOriginatorId(request)

        assertForbidden(result)
      }

      "GovUkOriginatorId is shorter than 3 characters" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "A" * 2)
        val result  = testSchemaValidationService.validateGovUkOriginatorId(request)

        assertForbidden(result)
      }

      "GovUkOriginatorId is longer than 40 characters" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "A" * 41)
        val result  = testSchemaValidationService.validateGovUkOriginatorId(request)

        assertForbidden(result)
      }

      "originatorId contains a space" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "contains space")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        assertForbidden(result)
      }

      "originatorId contains a tab" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "tab\tchar")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        assertForbidden(result)
      }

      "originatorId contains a new line" in {
        val json    = Json.obj()
        val request = buildFakeRequest(payload = json, headers = GovUkOriginatorId -> "new\nline")
        val result  = testSchemaValidationService.validateOriginatorId(request)

        assertForbidden(result)
      }
    }
  }

  "SchemaValidationServiceSpec.validateLiabilityNotificationRequest" must {

    "return Right" when {

      for (recordType <- universalCreditRecordTypes) {
        s"given a valid 'Insert' Request of '$recordType' record type" in {
          val json    = insertDwpRequestJson(recordType.code)
          val request = buildFakeRequest(json, CorrelationId -> correlationId)
          val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          result mustBe Right((correlationId, json.as[InsertUniversalCreditLiability]))
        }

        s"given a valid 'Terminate' Request of '$recordType' record type" in {
          val json    = terminateDwpRequestJson(recordType.code)
          val request = buildFakeRequest(json, CorrelationId -> correlationId)
          val result  = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          result mustBe Right((correlationId, json.as[TerminateUniversalCreditLiability]))
        }
      }

    }

    "return Left (400 Bad Request)" when {

      for {
        recordType <- universalCreditRecordTypes
        field      <- requiredInsertDwpFields
      }
        s"given an 'Insert' Request of '$recordType' missing $field" in {
          val invalidJson = jsObjectWithout(insertDwpRequestJson(recordType.code), field)
          val request     = buildFakeRequest(invalidJson, CorrelationId -> correlationId)
          val result      = testSchemaValidationService.validateLiabilityNotificationRequest(request)

          assertBadRequest(result, field)
        }

      for {
        recordType <- universalCreditRecordTypes
        field      <- requiredTerminateDwpFields
      }
        s"given a 'Terminate' Request of '$recordType' missing $field" in {
          val invalidJson = jsObjectWithout(terminateDwpRequestJson(recordType.code), field)
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
