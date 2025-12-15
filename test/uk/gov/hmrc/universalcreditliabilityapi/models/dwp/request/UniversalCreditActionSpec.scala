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

package uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.universalcreditliabilityapi.helpers.JsonTestHelpers.extractJsErrorMessage

class UniversalCreditActionSpec extends AnyWordSpec with Matchers {

  "UniversalCreditAction" must {

    "successfully deserialise" when {

      "given valid JSON with an 'Insert' action" in {
        val result = JsString("Insert").validate[UniversalCreditAction]

        result mustBe JsSuccess(UniversalCreditAction.Insert)
      }

      "given valid JSON with a 'Terminate' action" in {
        val result = JsString("Terminate").validate[UniversalCreditAction]

        result mustBe JsSuccess(UniversalCreditAction.Terminate)
      }
    }

    "fail to deserialise" when {
      "given JSON contains an unknown action" in {
        val result = JsString("INVALID").validate[UniversalCreditAction]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Unknown Universal Credit Action")
      }

      "given JSON contains an action with invalid casing of 'Insert' (only expecting 'Insert')" in {
        val result = JsString("iNsErT").validate[UniversalCreditAction]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Unknown Universal Credit Action")
      }

      "given JSON contains an action with invalid casing of 'Terminate' (only expecting 'Terminate')" in {
        val result = JsString("tErMiNaTe").validate[UniversalCreditAction]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Unknown Universal Credit Action")
      }

      "given JSON is empty" in {
        val result = JsString("").validate[UniversalCreditAction]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Unknown Universal Credit Action")
      }

      "given JSON is not a JsString" in {
        val nonJsString: JsObject                   = Json.obj("action" -> "Insert")
        val result: JsResult[UniversalCreditAction] = nonJsString.validate[UniversalCreditAction]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Unknown Universal Credit Action")
      }
    }
  }
}
