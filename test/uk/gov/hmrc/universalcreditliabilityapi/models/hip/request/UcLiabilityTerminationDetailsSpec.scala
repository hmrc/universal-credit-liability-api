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
import play.api.libs.json.{JsValue, Json, JsError}
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType.{LCW_LCWRA, UC}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{UcLiabilityTerminationDetails}

import java.time.LocalDate

class UcLiabilityTerminationDetailsSpec extends AnyWordSpec with Matchers {

  "UcLiabilityTerminationDetails" must {

    "successfully serialise" when {

      "given valid JSON with 'LCW/LCWRA' record type" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2024-08-19",
            |    "liabilityEndDate": "2025-01-04"
            |}
            |""".stripMargin)

        val details = UcLiabilityTerminationDetails(
          universalCreditRecordType = LCW_LCWRA,
          liabilityStartDate = LocalDate.parse("2024-08-19"),
          liabilityEndDate = LocalDate.parse("2025-01-04")
        )

        Json.toJson(details) mustBe json
      }

      "given valid JSON with valid leap year date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2004-02-29",
            |    "liabilityEndDate": "2024-02-29"
            |}
            |""".stripMargin)

        val details = UcLiabilityTerminationDetails(
          universalCreditRecordType = LCW_LCWRA,
          liabilityStartDate = LocalDate.parse("2004-02-29"),
          liabilityEndDate = LocalDate.parse("2024-02-29")
        )

        Json.toJson(details) mustBe json
      }

      "given valid JSON with boundary date values" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "1900-01-01",
            |    "liabilityEndDate": "2099-12-31"
            |}
            |""".stripMargin
        )

        val details = UcLiabilityTerminationDetails(
          universalCreditRecordType = LCW_LCWRA,
          liabilityStartDate = LocalDate.parse("1900-01-01"),
          liabilityEndDate = LocalDate.parse("2099-12-31")
        )

        Json.toJson(details) mustBe json
      }

      "given valid JSON with another valid date combination" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "UC",
            |    "liabilityStartDate": "2002-10-10",
            |    "liabilityEndDate": "2020-01-01"
            |}
            |""".stripMargin
        )

        val details = UcLiabilityTerminationDetails(
          universalCreditRecordType = UC,
          liabilityStartDate = LocalDate.parse("2002-10-10"),
          liabilityEndDate = LocalDate.parse("2020-01-01")
        )

        Json.toJson(details) mustBe json
      }
    }

    "fail to deserialise" when {

      "given JSON contains invalid record type" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "INVALID",
            |    "liabilityStartDate": "2002-10-10",
            |    "liabilityEndDate": "2025-01-04"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]

      }

      "given JSON contains invalid liability start date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2024-02-30",
            |    "liabilityEndDate": "2025-01-04"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }

      "given JSON contains liability start date with an invalid format" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2002/10/10",
            |    "liabilityEndDate": "2025-01-04"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]

      }

      "given JSON contains invalid liability end date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2002-10-10",
            |    "liabilityEndDate": "2025-01-45"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }

      "given JSON contains liability end date with invalid format" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2002-10-10",
            |    "liabilityEndDate": "2025/01/04"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }

      "given JSON is missing a record type" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "liabilityStartDate": "2002-10-10",
            |    "liabilityEndDate": "2015-08-19"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }

      "given JSON is missing a liability start Date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityEndDate": "2015-08-19"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }

      "given JSON is missing a liability End Date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2025-02-29"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }

      "given JSON contains invalid leap year dates" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2001-02-29",
            |    "liabilityEndDate": "2025-02-29"
            |}
            |""".stripMargin
        )

        json.validate[UcLiabilityTerminationDetails] mustBe a[JsError]
      }
    }
  }
}
