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
import uk.gov.hmrc.universalcreditliabilityapi.connectors.HipConnector
import uk.gov.hmrc.universalcreditliabilityapi.models.errors.Failure
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.UniversalCreditAction
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.UniversalCreditAction.{Insert, Terminate}
import uk.gov.hmrc.universalcreditliabilityapi.services.SchemaValidationService
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ErrorCodes.ForbiddenCode
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.ForbiddenReason
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.OriginatorId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UcLiabilityNotificationController @Inject() (
  cc: ControllerComponents,
  ucLiabilityService: SchemaValidationService,
  hipConnector: HipConnector
)(implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  def submitLiabilityNotification(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    (for {
      _            <- validateOriginatorId(request)
      creditAction <- ucLiabilityService.validateLiabilityNotificationRequest(request)
    } yield creditAction match {
      case Insert =>
        val call = hipConnector.insertUcLiability(request)

        call.map(result =>
          Status(result.status)(result.body)
            .withHeaders(result.headers.toSeq.map((k, v) => (k, v.mkString(" "))): _*)
        )

      case Terminate =>
        val call = hipConnector.terminateUcLiability(request)

        call.map(result =>
          Status(result.status)(result.body)
            .withHeaders(result.headers.toSeq.map((k, v) => (k, v.mkString(" "))): _*)
        )

    }).merge
  }

  private def validateOriginatorId[T](request: Request[T]) =
    request.headers
      .get(OriginatorId)
      .filter(_ => true) // TODO: Add business logic for originator id here
      .toRight(
        Future.successful(
          Results.Forbidden(Json.toJson(Failure(reason = ForbiddenReason, code = ForbiddenCode)))
        )
      )
}
