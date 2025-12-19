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
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType.LCW_LCWRA
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, UniversalCreditLiabilityDetails}

class UniversalCreditLiabilityDetailsSpec extends AnyWordSpec with Matchers {


  "UniversalCreditLiabilityDetails" must {

    "successfully serialise" when {

      "given valid JSON with 'LCW/LCWRA' record type" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2025-08-19"
            |}
            |""".stripMargin)

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = "2002-10-10",
          liabilityStartDate = "2025-08-19"
        )

        Json.toJson(testInsertLiabilityRequest) mustBe json
      }

      "given valid JSON with valid leap year dates" in {
        val json: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-02-29",
            |    "liabilityStartDate": "2024-02-29"
            |}
            |""".stripMargin)

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = "2002-02-29",
          liabilityStartDate = "2024-02-29")

        val requestInsert = InsertLiabilityRequest(testInsertLiabilityRequest)

        Json.toJson(testInsertLiabilityRequest) mustBe json
      }

      "given valid JSON with boundary date values" in {
        val expectedJson: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "1900-01-01",
            |    "liabilityStartDate": "2099-12-31"
            |}
            |""".stripMargin
        )

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = "1900-01-01",
          liabilityStartDate = "2099-12-31"
        )

        val result: JsValue = Json.toJson(testInsertLiabilityRequest)

        result mustBe expectedJson
      }

      "given valid JSON with other record types" in {
        val expectedJson: JsValue = Json.parse(
          """
            |{
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2001-01-01",
            |    "liabilityStartDate": "2020-01-01"
            |}
            |""".stripMargin
        )

        val testInsertLiabilityRequest = UniversalCreditLiabilityDetails(
          universalCreditRecordType = LCW_LCWRA,
          dateOfBirth = "2001-01-01",
          liabilityStartDate = "2020-01-01"
        )

        val result: JsValue = Json.toJson(testInsertLiabilityRequest)

        result mustBe expectedJson
      }
    }
  }
}

