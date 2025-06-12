/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.universalcreditliabilityapi.actions

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.Ok
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends AnyWordSpec with GuiceOneAppPerSuite with MockitoSugar with Injecting with Matchers {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override implicit def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector)
    )
    .build()

  lazy val authAction: AuthAction = app.injector.instanceOf[AuthAction]

  def happyPathResult: Result = Ok("Happy Path Result")

  def callAction(
    block: Request[_] => Future[Result] = (_: Request[_]) => Future.successful(happyPathResult)
  ): Future[Result] =
    authAction.invokeBlock(FakeRequest(), block)

  "Auth Action" must {
    "execute the action when auth is successful" in {
      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(()))

      val blockFutureResult = callAction()

      await(blockFutureResult) mustBe happyPathResult
    }

    "return 401 with MISSING_CREDENTIALS when MissingBearerToken is throw" in {
      val testMessage = "testMessage"
      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.failed(MissingBearerToken(testMessage)))

      val blockFutureResult = callAction()

      status(blockFutureResult) mustBe Status.UNAUTHORIZED
      contentAsJson(blockFutureResult) mustBe expectedErrorJson(code = "MISSING_CREDENTIALS", message = testMessage)
    }

    "return 401 with INVALID_CREDENTIALS when BearerTokenExpired is thrown" in {
      val testMessage = "testMessage"
      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.failed(BearerTokenExpired(testMessage)))

      val blockFutureResult = callAction()

      status(blockFutureResult) mustBe Status.UNAUTHORIZED
      contentAsJson(blockFutureResult) mustBe expectedErrorJson(code = "INVALID_CREDENTIALS", message = testMessage)
    }

    "return 401 with INVALID_CREDENTIALS when InvalidBearerToken is thrown" in {
      val testMessage = "testMessage"
      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.failed(InvalidBearerToken(testMessage)))

      val blockFutureResult = callAction()

      status(blockFutureResult) mustBe Status.UNAUTHORIZED
      contentAsJson(blockFutureResult) mustBe expectedErrorJson(code = "INVALID_CREDENTIALS", message = testMessage)
    }

    "return 401 with INCORRECT_ACCESS_TOKEN_TYPE when UnsupportedAuthProvider is thrown" in {
      val testMessage = "testMessage"
      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.failed(UnsupportedAuthProvider(testMessage)))

      val blockFutureResult = callAction()

      status(blockFutureResult) mustBe Status.UNAUTHORIZED
      contentAsJson(blockFutureResult) mustBe expectedErrorJson(
        code = "INCORRECT_ACCESS_TOKEN_TYPE",
        message = testMessage
      )
    }

    "return 401 with UNAUTHORIZED when another AuthorisationException is thrown is thrown" in {
      val testMessage = "testMessage"

      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.failed(new AuthorisationException(testMessage) {}))

      val blockFutureResult = callAction()

      status(blockFutureResult) mustBe Status.UNAUTHORIZED
      contentAsJson(blockFutureResult) mustBe expectedErrorJson(code = "UNAUTHORIZED", message = testMessage)
    }

    "not handle any None AuthorisationException thrown by the block" in {
      val testError = BadRequestException("test error")
      when(
        mockAuthConnector.authorise(any[Predicate], any[Retrieval[Unit]]())(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(Future.successful(()))

      val err = intercept[BadRequestException] {
        val blockFutureResult = callAction(_ => Future.failed(testError))

        await(blockFutureResult)
      }

      err mustBe testError
    }

  }

  def expectedErrorJson(code: String, message: String): JsValue =
    Json.parse(s"""
        |{
        | "code": "$code",
        | "message": "$message"
        |}
        |""".stripMargin)
}
