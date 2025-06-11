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
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.universalcreditliabilityapi.connectors.HipConnector
import uk.gov.hmrc.universalcreditliabilityapi.models.errors.Failure
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.UniversalCreditAction
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.UniversalCreditAction.{Insert, Terminate}
import uk.gov.hmrc.universalcreditliabilityapi.services.SchemaValidationService
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ErrorCodes.ForbiddenCode
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ForbiddenReason
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.OriginatorId

import javax.inject.Inject

@Singleton
class UcLiabilityNotificationController @Inject() (
  cc: ControllerComponents,
  ucLiabilityService: SchemaValidationService,
  hipConnector: HipConnector
) extends BackendController(cc) {

  def submitLiabilityNotification(): Action[JsValue] = Action(parse.json) { request =>
    (for {
      _            <- validateOriginatorId(request)
      creditAction <- ucLiabilityService.validateLiabilityNotificationRequest(request)
    } yield creditAction match {
      case Insert    =>
        hipConnector.insertUcLiability(request)
      case Terminate =>
        hipConnector.terminateUcLiability(request)
    }).merge
  }

  private def validateOriginatorId[T](request: Request[T]) =
    request.headers
      .get(OriginatorId)
      .filter(_ => true) // TODO: Add business logic for originator id here
      .toRight(Forbidden(Json.toJson(Failure(reason = ForbiddenReason, code = ForbiddenCode))))
}
