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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.universalcreditliabilityapi.models.common.UniversalCreditRecordType
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ValidationPatterns.{validDate, validNino}

case class TerminateUcLiabilityRequest(
  universalCreditAction: UniversalCreditAction,
  nationalInsuranceNumber: String,
  universalCreditRecordType: UniversalCreditRecordType,
  liabilityStartDate: String,
  liabilityEndDate: String
)

object TerminateUcLiabilityRequest {

  implicit val reads: Reads[TerminateUcLiabilityRequest] = (
    (JsPath \ "universalCreditAction").read[UniversalCreditAction] and
      (JsPath \ "nationalInsuranceNumber").read(validNino) and
      (JsPath \ "universalCreditRecordType").read[UniversalCreditRecordType] and
      (JsPath \ "liabilityStartDate").read(validDate) and
      (JsPath \ "liabilityEndDate").read(validDate)
  )(TerminateUcLiabilityRequest.apply _)

}
