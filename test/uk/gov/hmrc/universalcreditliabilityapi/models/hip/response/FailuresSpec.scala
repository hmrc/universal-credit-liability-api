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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, JsValue, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.Failures

class FailuresSpec extends AnyWordSpec with Matchers {

  "Failures" must {

    "successfully deserialise" when {

      "given JSON contains multiple 'failures'" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "failures":[
            |    { "code": "12345", "reason": "First error" },
            |    { "code": "54321", "reason": "Second error" }
            |  ]
            |}""".stripMargin)

        val expectedFailures: Failures =
          Failures(
            failures = Seq(
              Failure(code = "12345", reason = "First error"),
              Failure(code = "54321", reason = "Second error")
            )
          )

        val result = testJson.validate[Failures]

        result mustBe JsSuccess(expectedFailures, JsPath \ "failures")
      }

      "given JSON with empty 'failures'" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "failures":[]
            |}""".stripMargin)

        val expectedFailures: Failures = Failures(failures = Seq.empty[Failure])

        val result = testJson.validate[Failures]

        result mustBe JsSuccess(expectedFailures, JsPath \ "failures")
      }

    }

    "fail to deserialise" when {

      "given JSON is missing the 'failures'" in {
        val testJson: JsValue = Json.parse("""
            |{
            |  "":[]
            |}""".stripMargin)

        val result = testJson.validate[Failures]

        result mustBe a[JsError]
      }
    }

  }
}
