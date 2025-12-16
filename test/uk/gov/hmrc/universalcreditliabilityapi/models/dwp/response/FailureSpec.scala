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

package uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure

class FailureSpec extends AnyWordSpec with Matchers {

  "Failure" must {

    "serialise to the expected JSON object" in {
      val failureModel = Failure("12345", "Something went wrong")

      val failureJson: JsValue = Json.parse(
        """{
          | "code": "12345",
          | "message": "Something went wrong"
          |}""".stripMargin
      )

      Json.toJson(failureModel) mustBe failureJson
    }

    "contain the keys 'code' and 'message' when serialised" in {
      val failureModel = Failure("12345", "Something went wrong")

      val failureJsonKeys = Json.toJson(failureModel).as[JsObject].keys

      failureJsonKeys mustBe Set("code", "message")
    }
  }
}
