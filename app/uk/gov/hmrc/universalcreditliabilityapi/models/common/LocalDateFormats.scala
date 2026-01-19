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

package uk.gov.hmrc.universalcreditliabilityapi.models.common

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ValidationPatterns.DatePattern

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

object LocalDateFormats {

  given localDateReads: Reads[LocalDate] = Reads[LocalDate] { json =>
    json.validate[String].flatMap { dateStr =>
      if (DatePattern.matches(dateStr)) {
        Try(LocalDate.parse(dateStr)) match {
          case Success(localDate) => JsSuccess(localDate)
          case Failure(_)         => JsError("Date matches Regex pattern but can not be parsed into LocalDate")
        }
      } else {
        JsError("Date does not match the Regex pattern")
      }
    }
  }

  given localDateWrites: Writes[LocalDate] = Writes[LocalDate] { localDate =>
    JsString(localDate.toString)
  }

}
