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

package uk.gov.hmrc.universalcreditliabilityapi.support

import play.api.libs.json.{JsValue, Json}

import java.util.UUID
import scala.util.Random

object TestData {

  val testGovUkOriginatorId = "TEST-GOV-UK-ORIGINATOR-ID"
  val correlationId: String = UUID.randomUUID().toString

  val validHeaders: Map[String, String] = Map(
    "authorization"        -> "",
    "correlationId"        -> correlationId,
    "gov-uk-originator-id" -> testGovUkOriginatorId
  )

  def generateNino(): String = {
    val number = f"${Random.nextInt(100000)}%06d"
    val nino   = s"AA$number"
    nino
  }

  def validInsertionRequest(nino: String): JsValue = Json.parse(s"""
      |{
      |  "nationalInsuranceNumber": "$nino",
      |  "universalCreditRecordType": "LCW/LCWRA",
      |  "universalCreditAction": "Insert",
      |  "liabilityStartDate": "2025-08-19"
      |}
      |""".stripMargin)

  def validTerminateRequest(nino: String): JsValue = Json.parse(s"""
      |{
      |  "nationalInsuranceNumber": "$nino",
      |  "universalCreditRecordType": "LCW/LCWRA",
      |  "universalCreditAction": "Terminate",
      |  "liabilityStartDate": "2025-08-19",
      |  "liabilityEndDate": "2025-08-19"
      |}
      |""".stripMargin)

}
