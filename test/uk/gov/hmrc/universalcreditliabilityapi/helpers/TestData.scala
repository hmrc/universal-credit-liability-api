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

package uk.gov.hmrc.universalcreditliabilityapi.helpers

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.UniversalCreditAction.{Insert, Terminate}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.{InsertUniversalCreditLiability, TerminateUniversalCreditLiability, UniversalCreditAction}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, TerminateLiabilityRequest, UcLiabilityTerminationDetails, UniversalCreditLiabilityDetails}

import java.time.LocalDate
import java.util.UUID
import scala.util.Random

object TestData {

  val nino: String              = "AA123456"
  val correlationId: String     = UUID.randomUUID().toString
  val govUkOriginatorId: String = "TEST-GOV-UK-ORIGINATOR-ID"

  val universalCreditRecordTypes: Array[UniversalCreditRecordType] = UniversalCreditRecordType.values
  val universalCreditActions: Array[UniversalCreditAction]         = UniversalCreditAction.values

  private val randomRecordType: UniversalCreditRecordType = Random.shuffle(universalCreditRecordTypes).head

  val baseInsertDwpRequest: InsertUniversalCreditLiability =
    InsertUniversalCreditLiability(
      universalCreditAction = Insert,
      nationalInsuranceNumber = nino,
      universalCreditRecordType = randomRecordType,
      liabilityStartDate = LocalDate.parse("2024-01-15"),
      dateOfBirth = Some(LocalDate.parse("2002-04-27"))
    )

  val baseTerminateDwpRequest: TerminateUniversalCreditLiability =
    TerminateUniversalCreditLiability(
      universalCreditAction = Terminate,
      nationalInsuranceNumber = nino,
      universalCreditRecordType = randomRecordType,
      liabilityStartDate = LocalDate.parse("2024-01-15"),
      liabilityEndDate = LocalDate.parse("2024-12-31")
    )

  val baseInsertHipRequest: InsertLiabilityRequest =
    InsertLiabilityRequest(
      universalCreditLiabilityDetails = UniversalCreditLiabilityDetails(
        universalCreditRecordType = randomRecordType,
        liabilityStartDate = LocalDate.parse("2024-01-15"),
        dateOfBirth = Some(LocalDate.parse("2002-04-27"))
      )
    )

  val baseTerminateHipRequest: TerminateLiabilityRequest =
    TerminateLiabilityRequest(
      ucLiabilityTerminationDetails = UcLiabilityTerminationDetails(
        universalCreditRecordType = randomRecordType,
        liabilityStartDate = LocalDate.parse("2024-01-15"),
        liabilityEndDate = LocalDate.parse("2024-12-31")
      )
    )

  val requiredInsertDwpFields: Set[String] =
    Set(
      "universalCreditAction",
      "nationalInsuranceNumber",
      "universalCreditRecordType",
      "liabilityStartDate"
    )

  val requiredTerminateDwpFields: Set[String] =
    Set(
      "universalCreditAction",
      "nationalInsuranceNumber",
      "universalCreditRecordType",
      "liabilityStartDate",
      "liabilityEndDate"
    )

  val invalidInsertDwpRequestValues: Map[String, String] =
    Map(
      "universalCreditAction"     -> "INVALID",
      "nationalInsuranceNumber"   -> "INVALID",
      "universalCreditRecordType" -> "INVALID",
      "liabilityStartDate"        -> "not-a-date",
      "dateOfBirth"               -> "not-a-date"
    )

  val invalidTerminateDwpRequestValues: Map[String, String] =
    Map(
      "universalCreditAction"     -> "INVALID",
      "nationalInsuranceNumber"   -> "INVALID",
      "universalCreditRecordType" -> "INVALID",
      "liabilityStartDate"        -> "not-a-date",
      "liabilityEndDate"          -> "not-a-date"
    )

  def optionalField(key: String, value: Option[String]): JsObject =
    value.fold(Json.obj())(v => Json.obj(key -> v))

  def insertDwpRequestJson(
    recordType: String = randomRecordType.code,
    dateOfBirth: Option[String] = Some("2002-04-27")
  ): JsObject = Json.obj(
    "universalCreditAction"     -> "Insert",
    "nationalInsuranceNumber"   -> "AA123456",
    "universalCreditRecordType" -> recordType,
    "liabilityStartDate"        -> "2024-01-15"
  ) ++ optionalField("dateOfBirth", dateOfBirth)

  def terminateDwpRequestJson(recordType: String = randomRecordType.code): JsObject = Json.obj(
    "universalCreditAction"     -> "Terminate",
    "nationalInsuranceNumber"   -> "AA123456",
    "universalCreditRecordType" -> recordType,
    "liabilityStartDate"        -> "2024-01-15",
    "liabilityEndDate"          -> "2024-12-31"
  )

  val errorCodes: Seq[(String, String)] = Seq(
    ("55006", "Start Date and End Date must be earlier than Date of Death"),
    ("55008", "End Date must be earlier than State Pension Age"),
    ("55027", "End Date later than Date of Death"),
    ("55029", "Start Date later than SPA"),
    ("55038", "A conflicting or identical Liability is already recorded"),
    ("55039", "NO corresponding liability found"),
    ("64996", "Start Date is not before date of death"),
    ("64997", "LCW/LCWRA not within a period of UC"),
    ("64998", "LCW/LCWRA Override not within a period of LCW/LCWRA"),
    ("65026", "Start date must not be before 16th birthday"),
    ("65536", "Start date before 29/04/2013"),
    ("65537", "End date before start date"),
    ("65541", "The NINO input matches a Pseudo Account"),
    (
      "65542",
      "The NINO input matches a non-live account (including redundant, amalgamated and administrative account types)"
    ),
    ("65543", "The NINO input matches an account that has been transferred to the Isle of Man"),
    ("65544", "Account held on NPS, but has not gone through adult registration."),
    ("99999", "Start Date after Death")
  )

}
