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

package uk.gov.hmrc.universalcreditliabilityapi.utils

import play.api.libs.json.Reads
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure

import scala.util.matching.Regex

object ApplicationConstants {

  val ForbiddenReason = "Forbidden"

  object ValidationPatterns {
    val DatePattern: Regex =
      "^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$".r

    val CorrelationIdPattern: Regex =
      "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$".r

    val NinoPattern: Regex =
      "^([ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}$".r

    private def isValidNino(nino: String): Boolean = NinoPattern.matches(nino)

    val validNino: Reads[String] = Reads.verifying[String](isValidNino)

  }

  object HeaderNames {
    val Authorization = "Authorization"
    val CorrelationId = "correlationId"
    val OriginatorId  = "gov-uk-originator-id"
  }

  object ErrorCodes {
    val InvalidInput  = "400.1"
    val ForbiddenCode = "403.2"
  }

  def invalidInputFailure(field: String): Failure =
    Failure(
      message = ErrorMessages.invalidInput(field),
      code = ErrorCodes.InvalidInput
    )

  private object ErrorMessages {
    def invalidInput(field: String): String =
      s"Constraint Violation - Invalid/Missing input parameter: $field"
  }
}
