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

package uk.gov.hmrc.universalcreditliabilityapi.models.hip.request

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType.{LCW_LCWRA, UC}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.UcLiabilityTerminationDetails

import java.time.LocalDate

class UcLiabilityTerminationDetailsSpec extends AnyWordSpec with Matchers {

  "UcLiabilityTerminationDetails" must {

    "successfully serialise" when {

      "record type is 'UC'" in {
        val testTerminateLiabilityRequest: UcLiabilityTerminationDetails =
          UcLiabilityTerminationDetails(
            universalCreditRecordType = UC,
            liabilityStartDate = LocalDate.parse("2024-08-19"),
            liabilityEndDate = LocalDate.parse("2025-01-04")
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditRecordType": "UC",
            |  "liabilityStartDate": "2024-08-19",
            |  "liabilityEndDate": "2025-01-04"
            |}
            |""".stripMargin)

        Json.toJson(testTerminateLiabilityRequest) mustBe expectedJson
      }

      "record type is 'LCW/LCWRA'" in {
        val testTerminateLiabilityRequest: UcLiabilityTerminationDetails =
          UcLiabilityTerminationDetails(
            universalCreditRecordType = LCW_LCWRA,
            liabilityStartDate = LocalDate.parse("2024-08-19"),
            liabilityEndDate = LocalDate.parse("2025-01-04")
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2024-08-19",
            |  "liabilityEndDate": "2025-01-04"
            |}
            |""".stripMargin)

        Json.toJson(testTerminateLiabilityRequest) mustBe expectedJson
      }

      "dates include leap year values" in {
        val testTerminateLiabilityRequest: UcLiabilityTerminationDetails =
          UcLiabilityTerminationDetails(
            universalCreditRecordType = LCW_LCWRA,
            liabilityStartDate = LocalDate.parse("2000-02-29"),
            liabilityEndDate = LocalDate.parse("2024-02-29")
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "2000-02-29",
            |  "liabilityEndDate": "2024-02-29"
            |}
            |""".stripMargin)

        Json.toJson(testTerminateLiabilityRequest) mustBe expectedJson
      }

      "dates are at boundary values" in {
        val testTerminateLiabilityRequest: UcLiabilityTerminationDetails =
          UcLiabilityTerminationDetails(
            universalCreditRecordType = LCW_LCWRA,
            liabilityStartDate = LocalDate.parse("1900-01-01"),
            liabilityEndDate = LocalDate.parse("2099-12-31")
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditRecordType": "LCW/LCWRA",
            |  "liabilityStartDate": "1900-01-01",
            |  "liabilityEndDate": "2099-12-31"
            |}
            |""".stripMargin)

        Json.toJson(testTerminateLiabilityRequest) mustBe expectedJson
      }

      "start date with an invalid format" in
        Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2000/02/29",
            |    "liabilityEndDate": "2024-02-29"
            |  }
            |}
            |""".stripMargin)
      "end date with an invalid format" in
        Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2000-02-29",
            |    "liabilityEndDate": "2024/02/29"
            |  }
            |}
            |""".stripMargin)

    }
  }
}
