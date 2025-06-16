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

package uk.gov.hmrc.universalcreditliabilityapi.controllers

import jakarta.inject.Singleton
import play.api.Logger
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.universalcreditliabilityapi.actions.AuthAction
import uk.gov.hmrc.universalcreditliabilityapi.connectors.HipConnector
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.Failures as HipFailures
import uk.gov.hmrc.universalcreditliabilityapi.services.{MappingService, SchemaValidationService}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ErrorCodes.ForbiddenCode
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ForbiddenReason
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.OriginatorId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UcLiabilityNotificationController @Inject() (
  authAction: AuthAction,
  cc: ControllerComponents,
  validationService: SchemaValidationService,
  mappingService: MappingService,
  hipConnector: HipConnector
)(implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  private def logger = Logger(this.getClass).logger

  def submitLiabilityNotification(): Action[JsValue] = authAction.async(parse.json) { implicit request =>
    (for {
      originatorId                            <- validateOriginatorId(request)
      validatedRequest                        <- validationService.validateLiabilityNotificationRequest(request)
      (correlationId, requestObject)           = validatedRequest
      (nationalInsuranceNumber, mappedRequest) = mappingService.mapRequest(requestObject)
    } yield hipConnector
      .sendUcLiability(nationalInsuranceNumber, correlationId, originatorId, mappedRequest)
      .map { result =>
        result.status match {
          case NO_CONTENT           => NoContent
          case BAD_REQUEST          =>
            logger.warn("400 returned by HIP")
            InternalServerError
          case FORBIDDEN            =>
            Forbidden(
              Json.toJson(Failure(code = "403.2", message = "Forbidden"))
            )
          case NOT_FOUND            => NotFound
          case UNPROCESSABLE_ENTITY =>
            Json.parse(result.body).validate[HipFailures] match {
              case JsSuccess(hipResponse, _) =>
                val maybeResponse = mappingService.map422ResponseErrors(hipResponse)
                maybeResponse.map(response => UnprocessableEntity(Json.toJson(response))).getOrElse {
                  logger.warn("422 with no reasons returned by HIP")
                  InternalServerError
                }
              case _                         =>
                logger.warn("Unreadable 422 returned by HIP")
                InternalServerError
            }

          case SERVICE_UNAVAILABLE =>
            ServiceUnavailable(
              Json.toJson(
                Failure(
                  code = "SERVER_ERROR",
                  message = "The 'misc/universal-credit/liability' API is currently unavailable"
                )
              )
            )
          case _                   => InternalServerError
        }
      }).merge
  }

  private def validateOriginatorId[T](request: Request[T]) =
    request.headers
      .get(OriginatorId)
      .filter(_ => true)
      .toRight(
        Future.successful(
          Results.Forbidden(Json.toJson(Failure(message = ForbiddenReason, code = ForbiddenCode)))
        )
      )
}
