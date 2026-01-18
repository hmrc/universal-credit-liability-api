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
import play.api.mvc.Results.{BadRequest, Forbidden}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.UniversalCreditAction.{Insert, Terminate}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.request.{InsertUcLiabilityRequest, TerminateUcLiabilityRequest, UniversalCreditAction}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ValidationPatterns.CorrelationIdPattern

import scala.concurrent.Future

class SchemaValidationService {

  def validateOriginatorId[T](request: Request[T]): Either[Future[Result], String] =
    request.headers
      .get(HeaderNames.OriginatorId)
      .filter(_ => true) // FIXME: add expected OriginatorId value here e.g. `.filter(_ == "DWP_UC")`
      .toRight(
        Future.successful(
          Forbidden(Json.toJson(ApplicationConstants.forbiddenFailure))
        )
      )

  def validateLiabilityNotificationRequest(
    request: Request[JsValue]
  ): Either[Future[Result], (String, InsertUcLiabilityRequest | TerminateUcLiabilityRequest)] = {
    val correlationIdValidation: EitherNec[Failure, String] =
      validateCorrelationId(
        request.headers.get(HeaderNames.CorrelationId)
      )

    val actionValidation: Either[NonEmptyChain[Failure], InsertUcLiabilityRequest | TerminateUcLiabilityRequest] =
      determineActionType(request.body).flatMap {
        case Insert    => validateJson[InsertUcLiabilityRequest](request)
        case Terminate => validateJson[TerminateUcLiabilityRequest](request)
      }

    (correlationIdValidation, actionValidation)
      .parMapN((correlationId, requestObject) => (correlationId, requestObject))
      .leftMap(convertFirstBadRequestToResult)
  }

  private def validateCorrelationId(correlationId: Option[String]): EitherNec[Failure, String] =
    correlationId match {
      case Some(id) if CorrelationIdPattern.matches(id) => Right(id)
      case _                                            =>
        Left(
          NonEmptyChain.one(
            ApplicationConstants.invalidInputFailure(HeaderNames.CorrelationId)
          )
        )
    }

  private def determineActionType(json: JsValue): Either[NonEmptyChain[Failure], UniversalCreditAction] =
    (json \ "universalCreditAction").validate[UniversalCreditAction] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors)     =>
        Left(
          NonEmptyChain.one(
            ApplicationConstants.invalidInputFailure("universalCreditAction")
          )
        )
    }

  private def validateJson[T](request: Request[JsValue])(implicit reads: Reads[T]): EitherNec[Failure, T] = {
    val fieldOrder: List[String] = List(
      "nationalInsuranceNumber",
      "universalCreditRecordType",
      "dateOfBirth",
      "liabilityStartDate",
      "liabilityEndDate"
    )

    request.body.validate[T] match {
      case JsSuccess(validatedRequest, _) =>
        Right(validatedRequest)

      case JsError(errors) =>
        val unorderedFailures: Seq[(String, Failure)] = errors.flatMap { case (path, validationErrors) =>
          val field: String = path.toString().stripPrefix("/")
          validationErrors.map(_ => field -> ApplicationConstants.invalidInputFailure(field))
        }.toSeq

        val orderedFailures: Seq[Failure] = unorderedFailures
          .sortBy { case (field, _) =>
            fieldOrder.indexOf(field) match {
              case idx => idx
            }
          }
          .map(_._2)

        Left(NonEmptyChain.fromSeq(orderedFailures).get)
    }
  }

  private def convertFirstBadRequestToResult(badRequestFailures: NonEmptyChain[Failure]): Future[Result] = {
    val allBadRequestFailures: Seq[Failure] = badRequestFailures.toList
    Future.successful(
      BadRequest(Json.toJson(allBadRequestFailures.head))
    )
  }

}
