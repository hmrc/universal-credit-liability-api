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

package uk.gov.hmrc.universalcreditliabilityapi.actions

import play.api.libs.json.Json
import play.api.mvc.Results.Unauthorized
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject() (
  val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[Request, AnyContent]
    with ActionFunction[Request, Request]
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    given Request[A]               = request
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised(AuthProviders(AuthProvider.PrivilegedApplication))(block(request))
      .recover {
        case err: MissingBearerToken                        =>
          Unauthorized(Json.toJson(Failure(code = AuthConstants.MISSING_CREDENTIALS, message = err.getMessage)))
            .withHeaders(generateResponseHeader)
        case err: (BearerTokenExpired | InvalidBearerToken) =>
          Unauthorized(Json.toJson(Failure(code = AuthConstants.INVALID_CREDENTIALS, message = err.getMessage)))
            .withHeaders(generateResponseHeader)
        case err: UnsupportedAuthProvider                   =>
          Unauthorized(Json.toJson(Failure(code = AuthConstants.INCORRECT_ACCESS_TOKEN_TYPE, message = err.getMessage)))
            .withHeaders(generateResponseHeader)
        case err: AuthorisationException                    =>
          Unauthorized(Json.toJson(Failure(code = AuthConstants.UNAUTHORIZED, message = err.getMessage)))
            .withHeaders(generateResponseHeader)
      }
  }

  private def generateResponseHeader[A](using request: Request[A]): (String, String) =
    "correlationId" -> request.headers.get("correlationId").getOrElse("")
}

object AuthConstants {
  val MISSING_CREDENTIALS         = "MISSING_CREDENTIALS"
  val INVALID_CREDENTIALS         = "INVALID_CREDENTIALS"
  val INCORRECT_ACCESS_TOKEN_TYPE = "INCORRECT_ACCESS_TOKEN_TYPE"
  val UNAUTHORIZED                = "UNAUTHORIZED"
}
