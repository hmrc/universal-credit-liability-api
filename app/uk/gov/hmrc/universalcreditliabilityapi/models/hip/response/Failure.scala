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

package uk.gov.hmrc.universalcreditliabilityapi.models.hip.response

import play.api.libs.functional.syntax.*
import play.api.libs.json.*

final case class Failure(code: String, reason: String)

object Failure {
  given Reads[Failure] = (
    (JsPath \ "code").read[String] and
      (JsPath \ "reason").read[String]
  )(Failure.apply _)
}
