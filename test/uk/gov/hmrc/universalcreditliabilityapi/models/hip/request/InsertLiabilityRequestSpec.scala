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
import play.api.libs.json.{JsError, JsValue, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType.{LCW_LCWRA, UC}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, UniversalCreditLiabilityDetails}

import java.time.LocalDate

class InsertLiabilityRequestSpec extends AnyWordSpec with Matchers {

  "InsertLiabilityRequest" must {

    "successfully serialise" when {

      "given valid JSON with 'LCW/LCWRA' record type" in {
        val json: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2025-08-19"
            |  }
            |}
            |""".stripMargin)

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = LocalDate.parse("2002-10-10"),
          liabilityStartDate = LocalDate.parse("2025-08-19")
        )

        val request = InsertLiabilityRequest(testInsertLiabilityRequest)

        Json.toJson(request) mustBe json
      }

      "given valid JSON with valid leap year dates" in {
        val json: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2004-02-29",
            |    "liabilityStartDate": "2024-02-29"
            |  }
            |}
            |""".stripMargin)

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = LocalDate.parse("2004-02-29"),
          liabilityStartDate = LocalDate.parse("2024-02-29")
        )

        val request = InsertLiabilityRequest(testInsertLiabilityRequest)

        Json.toJson(request) mustBe json
      }

      "given valid JSON with boundary date values" in {
        val expectedJson: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "1900-01-01",
            |    "liabilityStartDate": "2099-12-31"
            |  }
            |}
            |""".stripMargin
        )

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = LocalDate.parse("1900-01-01"),
          liabilityStartDate = LocalDate.parse("2099-12-31")
        )

        val request = InsertLiabilityRequest(testInsertLiabilityRequest)

        Json.toJson(request) mustBe expectedJson
      }

      "given valid JSON with another valid date combination" in {
        val expectedJson: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "UC",
            |    "dateOfBirth": "2001-01-01",
            |    "liabilityStartDate": "2020-01-01"
            |  }
            |}
            |""".stripMargin
        )

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = UC,
          dateOfBirth = LocalDate.parse("2001-01-01"),
          liabilityStartDate = LocalDate.parse("2020-01-01")
        )

        val request = InsertLiabilityRequest(testInsertLiabilityRequest)

        Json.toJson(request) mustBe expectedJson
      }
    }
    "fail to deserialise" when {

      "given JSON contains invalid record type" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "INVALID",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2015-08-19"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]

      }

      "given JSON contains invalid liability start date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2025-12-45"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON contains liability start date with an invalid format" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2025/12/12"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON contains invalid date of birth" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-45",
            |    "liabilityStartDate": "2025-12-12"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON contains date of birth with invalid format" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002/10/10",
            |    "liabilityStartDate": "2025-12-12"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON is missing a record type" in {
        val json: JsValue = Json.parse(
          """
            |{
            |   "universalCreditLiabilityDetails": {
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2015-08-19"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON is missing a date of birth" in {
        val json: JsValue = Json.parse(
          """
            |{
            |   "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2015-08-19"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON is missing a liability Start Date" in {
        val json: JsValue = Json.parse(
          """
            |{
            |   "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-10"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }

      "given JSON contains invalid leap year dates" in {
        val json: JsValue = Json.parse(
          """
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2001-02-29",
            |    "liabilityStartDate": "2025-02-29"
            |  }
            |}
            |""".stripMargin
        )

        json.validate[InsertLiabilityRequest] mustBe a[JsError]
      }
    }
  }
}
