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
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.Failures

class FailuresSpec extends AnyWordSpec with Matchers {

  "Failures" must {
    "successfully deserialise from valid JSON through a sequence" in {
      val failureModel = Json.parse(
        """{
          |"failures":[
          | {"code": "12345", "reason": "Something went wrong"},
          | {"code": "54321", "reason": "Something went wrong"}
          | ]
          |}""".stripMargin
      )

      val failureJson: Failures = failureModel.as[Failures]
      failureJson mustBe Failures(Seq(
        Failure("12345", "Something went wrong"),
        Failure("54321", "Something went wrong")
      ))
    }

    "fail to deserialise when failures is empty" in {
      val failureModel = Json.parse(
        """{
          |"failures":[]
          |}""".stripMargin
      )
      val failureJson: Failures = failureModel.as[Failures]
      failureJson mustBe Failures(Seq.empty)
    }

    "fail to deserialise when 'failures' is missing" in {
      val failureModel = Json.parse(
        """{
          |"":[]
          |}""".stripMargin
      )
      assertThrows[JsResultException] {
        failureModel.as[Failures]
      }
    }
  }
}
