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
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, UniversalCreditLiabilityDetails}

import java.time.LocalDate

class InsertLiabilityRequestSpec extends AnyWordSpec with Matchers {

  "InsertLiabilityRequest" must {

    "successfully serialise" when {

      "record type is 'UC'" in {
        val testInsertLiabilityRequest: InsertLiabilityRequest =
          InsertLiabilityRequest(
            UniversalCreditLiabilityDetails(
              universalCreditRecordType = UC,
              dateOfBirth = Some(LocalDate.parse("2002-10-10")),
              liabilityStartDate = LocalDate.parse("2025-08-19")
            )
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "UC",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2025-08-19"
            |  }
            |}
            |""".stripMargin)

        Json.toJson(testInsertLiabilityRequest) mustBe expectedJson
      }

      "record type is 'LCW/LCWRA'" in {
        val testInsertLiabilityRequest: InsertLiabilityRequest =
          InsertLiabilityRequest(
            UniversalCreditLiabilityDetails(
              universalCreditRecordType = LCW_LCWRA,
              dateOfBirth = Some(LocalDate.parse("2002-10-10")),
              liabilityStartDate = LocalDate.parse("2025-08-19")
            )
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2002-10-10",
            |    "liabilityStartDate": "2025-08-19"
            |  }
            |}
            |""".stripMargin)

        Json.toJson(testInsertLiabilityRequest) mustBe expectedJson
      }

      "dates include leap year values" in {
        val testInsertLiabilityRequest: InsertLiabilityRequest =
          InsertLiabilityRequest(
            UniversalCreditLiabilityDetails(
              universalCreditRecordType = LCW_LCWRA,
              dateOfBirth = Some(LocalDate.parse("2000-02-29")),
              liabilityStartDate = LocalDate.parse("2024-02-29")
            )
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "2000-02-29",
            |    "liabilityStartDate": "2024-02-29"
            |  }
            |}
            |""".stripMargin)

        Json.toJson(testInsertLiabilityRequest) mustBe expectedJson
      }

      "dates are at boundary values" in {
        val testInsertLiabilityRequest: InsertLiabilityRequest =
          InsertLiabilityRequest(
            UniversalCreditLiabilityDetails(
              universalCreditRecordType = LCW_LCWRA,
              dateOfBirth = Some(LocalDate.parse("1900-01-01")),
              liabilityStartDate = LocalDate.parse("2099-12-31")
            )
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "1900-01-01",
            |    "liabilityStartDate": "2099-12-31"
            |  }
            |}
            |""".stripMargin)

        Json.toJson(testInsertLiabilityRequest) mustBe expectedJson
      }
    }

    "Handle optional dateOfBirth" when {

      "dateOfBirth is None" in {
        val testInsertLiabilityRequest: InsertLiabilityRequest =
          InsertLiabilityRequest(
            UniversalCreditLiabilityDetails(
              universalCreditRecordType = LCW_LCWRA,
              dateOfBirth = None,
              liabilityStartDate = LocalDate.parse("2024-01-15")
            )
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "liabilityStartDate": "2024-01-15"
            |  }
            |}
            |""".stripMargin)

        Json.toJson(testInsertLiabilityRequest) mustBe expectedJson
      }

      "dateOfBirth is valid" in {
        val testInsertLiabilityRequest: InsertLiabilityRequest =
          InsertLiabilityRequest(
            UniversalCreditLiabilityDetails(
              universalCreditRecordType = LCW_LCWRA,
              dateOfBirth = Some(LocalDate.parse("1990-05-20")),
              liabilityStartDate = LocalDate.parse("2024-01-15")
            )
          )

        val expectedJson: JsValue = Json.parse("""
            |{
            |  "universalCreditLiabilityDetails": {
            |    "universalCreditRecordType": "LCW/LCWRA",
            |    "dateOfBirth": "1990-05-20",
            |    "liabilityStartDate": "2024-01-15"
            |  }
            |}
            |""".stripMargin)

        Json.toJson(testInsertLiabilityRequest) mustBe expectedJson
      }
    }
  }
}
