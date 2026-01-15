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

package uk.gov.hmrc.universalcreditliabilityapi.helpers

import org.scalatest.Assertions.fail
import play.api.libs.json.{JsError, JsObject, JsResult, JsValue}
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest

import scala.concurrent.Future

object TestHelpers {

  def buildFakeRequest(payload: JsValue, headers: (String, String)*): Request[JsValue] =
    FakeRequest()
      .withHeaders(headers: _*)
      .withBody(payload)

  // Extracts the error message of a JsError
  def extractJsErrorMessage(jsResult: JsResult[_]): Option[String] =
    jsResult match {
      case JsError(errors) => errors.headOption.flatMap(_._2.headOption.map(_.message))
      case _               => None
    }

  // Extracts Future[Result] from Left, fails test if Right
  def extractLeftOrFail(result: Either[Future[Result], _]): Future[Result] = result match {
    case Left(errorFuture) => errorFuture
    case Right(success)    => fail(s"Expected Left (Failure), but got Right: $success")
  }

  // Removes a field from a JsObject
  def jsObjectWithout(baseJsObject: JsObject, field: String): JsObject = baseJsObject - field

  // Updates a fields of a JsObject
  def jsObjectWith(baseJsObject: JsObject, field: String, value: JsValue): JsObject = baseJsObject + (field -> value)

}
