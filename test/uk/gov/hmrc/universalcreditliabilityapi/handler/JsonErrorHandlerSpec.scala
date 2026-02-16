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

package uk.gov.hmrc.universalcreditliabilityapi.handler

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestData.correlationId
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.CorrelationId

class JsonErrorHandlerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  private val fakeRequestHeader: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withHeaders("Accept" -> "application/json")

  private val jsonErrorHandler: JsonErrorHandler = app.injector.instanceOf[JsonErrorHandler]

  "JsonErrorHandler.onClientError" must {
    "return 404 with empty body and CorrelationId header" in {
      val request = fakeRequestHeader.withHeaders(CorrelationId -> correlationId)
      val result  = jsonErrorHandler.onClientError(request, NOT_FOUND, "URI not found")

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe ""
      header(CorrelationId, result) mustBe Some(correlationId)
    }

    "return 404 with empty body and generated CorrelationId when header is missing" in {
      val result = jsonErrorHandler.onClientError(fakeRequestHeader, NOT_FOUND, "URI not found")

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe ""
      header(CorrelationId, result) mustBe None
    }

    "return the same code and body for any non-404 error e.g. 403" in {
      val request = fakeRequestHeader.withHeaders(CorrelationId -> correlationId)
      val result  = jsonErrorHandler.onClientError(request, FORBIDDEN, "Forbidden")

      status(result) mustBe FORBIDDEN
      contentAsString(result) must not be empty
      header(CorrelationId, result) mustBe None // correlationId added by CorrelationIdFilter
    }

  }

  "JsonErrorHandler.onServerError" must {

    "return 500 with empty body when CorrelationId header is present" in {
      val request = fakeRequestHeader.withHeaders(CorrelationId -> correlationId)
      val result  = jsonErrorHandler.onServerError(request, new RuntimeException("test error"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe ""
      header(CorrelationId, result) mustBe Some(correlationId)
    }

    "return 500 with empty body when CorrelationId header is missing" in {
      val result = jsonErrorHandler.onServerError(fakeRequestHeader, new RuntimeException("test error"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe ""
      header(CorrelationId, result) mustBe None // correlationId added by CorrelationIdFilter
    }
  }
}
