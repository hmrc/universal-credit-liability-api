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

package uk.gov.hmrc.universalcreditliabilityapi.services

import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.{InsertUcLiabilityRequest, TerminateUcLiabilityRequest}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, TerminateLiabilityRequest, UcLiabilityTerminationDetails, UniversalCreditLiabilityDetails}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.{Failures => HipFailures}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.{Failure => DwpFailure}

class MappingService {

  def mapRequest(
    request: InsertUcLiabilityRequest | TerminateUcLiabilityRequest
  ): (String, InsertLiabilityRequest | TerminateLiabilityRequest) = request match {
    case insert: InsertUcLiabilityRequest       =>
      (
        insert.nationalInsuranceNumber,
        InsertLiabilityRequest(
          UniversalCreditLiabilityDetails(
            universalCreditRecordType = insert.universalCreditRecordType,
            dateOfBirth = insert.dateOfBirth,
            liabilityStartDate = insert.liabilityStartDate
          )
        )
      )
    case terminate: TerminateUcLiabilityRequest =>
      (
        terminate.nationalInsuranceNumber,
        TerminateLiabilityRequest(
          UcLiabilityTerminationDetails(
            universalCreditRecordType = terminate.universalCreditRecordType,
            liabilityStartDate = terminate.liabilityStartDate,
            liabilityEndDate = terminate.liabilityEndDate
          )
        )
      )
  }

  def map422ResponseErrors(response: HipFailures): Option[DwpFailure] =
    response.failures.map(f => DwpFailure(code = f.code, message = f.reason)).headOption
}
