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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestHelpers.extractJsErrorMessage
import uk.gov.hmrc.universalcreditliabilityapi.models.common.LocalDateFormats

import java.time.LocalDate

class LocalDateFormatsSpec extends AnyWordSpec with Matchers {

  given Reads[LocalDate] = LocalDateFormats.localDateReads

  given Writes[LocalDate] = LocalDateFormats.localDateWrites

  "LocalDateFormats" must {

    "successfully deserialise" when {
      "the date has a valid ISO-8601 format (yyyy-MM-dd)" in {
        val testJson          = JsString("2025-11-23")
        val expectedLocalDate = LocalDate.of(2025, 11, 23)

        val result = testJson.validate[LocalDate]

        result mustBe JsSuccess(expectedLocalDate)
      }

      "the date has a valid ISO-8601 format with leading zeros for month and day" in {
        val testJson          = JsString("2025-04-01")
        val expectedLocalDate = LocalDate.of(2025, 4, 1)

        val result = testJson.validate[LocalDate]

        result mustBe JsSuccess(expectedLocalDate)
      }

      "the date is a valid leap-year date" in {
        val testJson          = JsString("2000-02-29")
        val expectedLocalDate = LocalDate.of(2000, 2, 29)

        val result = testJson.validate[LocalDate]

        result mustBe JsSuccess(expectedLocalDate)
      }
    }

    "fail to deserialise" when {

      "the date has an invalid ISO-8601 format (dd-MM-yyyy)" in {
        val testJson = JsString("23-11-2025")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the month and day of the date are missing leading zeros" in {
        val testJson = JsString("2025-4-1")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is an invalid leap-year date" in {
        val testJson = JsString("2025-02-29")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date has dots as separators" in {
        val testJson = JsString("2025.02.29")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date has slashes as separators" in {
        val testJson = JsString("2025/02/29")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date has other special characters as separators" in {
        val testJson = JsString("2025#02#29")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is below the valid range of dates" in {
        val testJson = JsString("1025/12/28")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is above the valid range of dates" in {
        val testJson = JsString("3025/12/28")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is logically invalid" in {
        val testJson = JsString("2011-22-33")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is non-numeric" in {
        val testJson = JsString("yyyy-MM-dd")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is an empty String" in {
        val testJson = JsString("")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }

      "the date is an invalid String" in {
        val testJson = JsString("INVALID")

        val result = testJson.validate[LocalDate]

        result mustBe a[JsError]
        extractJsErrorMessage(result) mustBe Some("Date does not match the Regex pattern")
      }
    }

    "successfully serialise a date" when {
      "converting a LocalDate to a Json String date" in {
        val testLocalDate: LocalDate = LocalDate.of(2025, 11, 28)
        val expectedResult: JsString = JsString("2025-11-28")

        val result: JsValue = Json.toJson(testLocalDate)

        result mustBe expectedResult
      }
    }
  }

}
