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
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType.{LCW_LCWRA, UC}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.UniversalCreditAction.Terminate

class TerminateLiabilityRequestSpec extends AnyWordSpec with Matchers {

  "TerminateUcLiabilityRequest" must {

    "successfully deserialise" when {

      "given valid JSON with 'UC' record type" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "UC",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val expectedTerminateUcLiabilityRequest: TerminateUcLiabilityRequest =
          TerminateUcLiabilityRequest(
            universalCreditAction = Terminate,
            nationalInsuranceNumber = "AA123456",
            universalCreditRecordType = UC,
            liabilityStartDate = "2025-12-15",
            liabilityEndDate = "2026-12-15"
          )

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe JsSuccess(expectedTerminateUcLiabilityRequest)
      }

      "given valid JSON with 'LCW/LCWRA' record type" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val expectedTerminateUcLiabilityRequest: TerminateUcLiabilityRequest =
          TerminateUcLiabilityRequest(
            universalCreditAction = Terminate,
            nationalInsuranceNumber = "AA123456",
            universalCreditRecordType = LCW_LCWRA,
            liabilityStartDate = "2025-12-15",
            liabilityEndDate = "2026-12-15"
          )

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe JsSuccess(expectedTerminateUcLiabilityRequest)
      }

      "given valid JSON with valid leap year dates" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2024-02-29",
            |  "liabilityEndDate": "2000-02-29"
            |}
            |""".stripMargin)

        val expectedTerminateUcLiabilityRequest: TerminateUcLiabilityRequest =
          TerminateUcLiabilityRequest(
            universalCreditAction = Terminate,
            nationalInsuranceNumber = "AA123456",
            universalCreditRecordType = LCW_LCWRA,
            liabilityStartDate = "2024-02-29",
            liabilityEndDate = "2000-02-29"
          )

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe JsSuccess(expectedTerminateUcLiabilityRequest)
      }

    }

    "fail to deserialise" when {

      "given JSON contains invalid record type" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "INVALID",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains invalid NINO (too short)" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA1234",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains invalid NINO (too long)" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA1234567890",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains invalid NINO (invalid prefix)" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "DA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains invalid liability start date" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-45",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains liability start date with an invalid format" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025/12/15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains invalid liability end date" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-45"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains liability end date with an invalid format" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026/12/15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON is missing a NINO" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON is missing an action" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON is missing a record type" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "liabilityStartDate": "2025-12-15",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON is missing a liability start date" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityEndDate": "2026-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON is missing a liability end date" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-12-15"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

      "given JSON contains invalid leap year dates" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "universalCreditAction": "Terminate",
            |  "nationalInsuranceNumber": "AA123456",
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2025-02-29",
            |  "liabilityEndDate": "2001-02-29"
            |}
            |""".stripMargin)

        val result = testJson.validate[TerminateUcLiabilityRequest]

        result mustBe a[JsError]
      }

    }

  }

}
