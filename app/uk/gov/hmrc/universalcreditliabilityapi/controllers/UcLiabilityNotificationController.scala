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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.universalcreditliabilityapi.actions.AuthAction
import uk.gov.hmrc.universalcreditliabilityapi.connectors.HipConnector
import uk.gov.hmrc.universalcreditliabilityapi.models.errors.Failure
import uk.gov.hmrc.universalcreditliabilityapi.services.{MappingService, SchemaValidationService}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ErrorCodes.ForbiddenCode
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ForbiddenReason
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.{CorrelationId, OriginatorId}

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

  def submitLiabilityNotification(): Action[JsValue] = authAction.async(parse.json) { implicit request =>
    (for {
      originatorId                            <- validateOriginatorId(request)
      validatedRequest                        <- validationService.validateLiabilityNotificationRequest(request)
      (correlationId, requestObject)           = validatedRequest
      (nationalInsuranceNumber, mappedRequest) = mappingService.map(requestObject)
    } yield hipConnector
      .sendUcLiability(nationalInsuranceNumber, correlationId, originatorId, mappedRequest)
      .map(result => Status(result.status)(result.body))).merge
      .map(_.withHeaders(CorrelationId -> request.headers.get(CorrelationId).getOrElse("")))
  }

  private def validateOriginatorId[T](request: Request[T]) =
    request.headers
      .get(OriginatorId)
      .filter(_ => true)
      .toRight(
        Future.successful(
          Results.Forbidden(Json.toJson(Failure(reason = ForbiddenReason, code = ForbiddenCode)))
        )
      )
}
