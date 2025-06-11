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

package uk.gov.hmrc.universalcreditliabilityapi.services

import cats.data.{EitherNec, NonEmptyChain}
import cats.syntax.all.*
import play.api.libs.json.*
import play.api.mvc.Results.BadRequest
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.universalcreditliabilityapi.models.errors.{Failure, Failures}
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.UniversalCreditAction.{Insert, Terminate}
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.{InsertUcLiabilityRequest, TerminateUcLiabilityRequest, UniversalCreditAction}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ValidationPatterns.CorrelationIdPattern

import scala.concurrent.Future

class SchemaValidationService {

  def validateLiabilityNotificationRequest(
    request: Request[JsValue]
  ): Either[Future[Result], (String, InsertUcLiabilityRequest | TerminateUcLiabilityRequest)] = {
    val correlationIdValidation: EitherNec[Failures, String]                                                      = validateCorrelationId(
      request.headers.get(HeaderNames.CorrelationId)
    )
    val actionValidation: Either[NonEmptyChain[Failures], InsertUcLiabilityRequest | TerminateUcLiabilityRequest] =
      determineActionType(request.body).flatMap {
        case Insert    =>
          validateJson[InsertUcLiabilityRequest](request)
        case Terminate =>
          validateJson[TerminateUcLiabilityRequest](request)
      }

    (correlationIdValidation, actionValidation)
      .parMapN((correlationId, requestObject) => (correlationId, requestObject))
      .leftMap(mergeFailures)
  }

  private def validateCorrelationId(correlationId: Option[String]): EitherNec[Failures, String] =
    correlationId match {
      case Some(id) if CorrelationIdPattern.matches(id) => Right(id)
      case _                                            =>
        Left(
          NonEmptyChain.one(
            Failures(
              failures = Seq(
                ApplicationConstants.invalidInputFailure(HeaderNames.CorrelationId)
              )
            )
          )
        )
    }

  private def determineActionType(json: JsValue): Either[NonEmptyChain[Failures], UniversalCreditAction] =
    (json \ "universalCreditAction").validate[UniversalCreditAction] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors)     =>
        Left(
          NonEmptyChain.one(
            Failures(
              failures = Seq(
                ApplicationConstants.invalidInputFailure("universalCreditAction")
              )
            )
          )
        )
    }

  private def validateJson[T](request: Request[JsValue])(implicit reads: Reads[T]): EitherNec[Failures, T] =
    request.body.validate[T] match {
      case JsSuccess(validatedRequest, _) =>
        Right(validatedRequest)

      case JsError(errors) =>
        val failures = errors.flatMap { case (path, validationErrors) =>
          val field = path.toString().stripPrefix("/")
          validationErrors.map { err =>
            ApplicationConstants.invalidInputFailure(field)
          }
        }.toSeq

        Left(
          NonEmptyChain.one(
            Failures(failures)
          )
        )
    }

  private def mergeFailures(failures: NonEmptyChain[Failures]): Future[Result] = {
    val allFailures: Seq[Failure] = failures.toList.flatMap(_.failures)
    Future.successful(BadRequest(Json.toJson(Failures(allFailures))))
  }
}
