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

package uk.gov.hmrc.universalcreditliabilityapi.models.common

import play.api.libs.json.*

enum UniversalCreditRecordType(val code: String) {
  case UC extends UniversalCreditRecordType("UC")
  case LCW_LCWRA extends UniversalCreditRecordType("LCW/LCWRA")
}

object UniversalCreditRecordType {
  implicit val writes: Writes[UniversalCreditRecordType] = Writes(recordType => JsString(recordType.code))

  implicit val reads: Reads[UniversalCreditRecordType] = Reads {
    case JsString("UC")        => JsSuccess(UniversalCreditRecordType.UC)
    case JsString("LCW/LCWRA") => JsSuccess(UniversalCreditRecordType.LCW_LCWRA)
    case _                     => JsError("Unknown RecordType")
  }
}
