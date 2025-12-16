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
import play.api.libs.json.{JsResult, JsResultException, Json}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.Failure

class FailureSpec extends AnyWordSpec with Matchers {

  "Failure" must {

    "successfully deserialise from valid JSON" in {
      val failureModel = Json.parse(
        """{
          | "code": "12345",
          | "reason": "Something went wrong"
          |}""".stripMargin
      )

      val failureJson: Failure = failureModel.as[Failure]
      failureJson mustBe Failure("12345", "Something went wrong")
    }

    "fail to deserialise when 'reason' is missing" in {
      val failureModel = Json.parse(
        """{
          |"code":"12345"
          |}""".stripMargin
      )
      assertThrows[JsResultException] {
        failureModel.as[Failure]
      }
    }

    "fail to deserialise when 'code' is missing" in {
      val failureModel = Json.parse(
        """{
          |"reason":"Something went wrong"
          |}""".stripMargin
      )
      assertThrows[JsResultException] {
        failureModel.as[Failure]
      }
    }

  }

}
