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
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestData.correlationId
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.CorrelationId

import scala.concurrent.Future

class JsonErrorHandlerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val requestHeader: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("Accept" -> "application/json")
  val handler: JsonErrorHandler = app.injector.instanceOf[JsonErrorHandler]

  "JsonErrorHandler" must {

    "return 500 with CorrelationId header when present" in {
      val req: FakeRequest[AnyContentAsEmpty.type] = requestHeader.withHeaders(CorrelationId -> correlationId)
      val result: Future[Result] = handler.onServerError(req, new RuntimeException("test error"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      header(CorrelationId, result) mustBe Some(correlationId)
    }

    "return 500 without CorrelationId header when missing" in {
      val result: Future[Result] = handler.onServerError(requestHeader, new RuntimeException("test error"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      header(CorrelationId, result) mustBe None
    }
  }
}
