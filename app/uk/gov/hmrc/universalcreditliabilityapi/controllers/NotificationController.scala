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

import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.universalcreditliabilityapi.actions.AuthAction
import uk.gov.hmrc.universalcreditliabilityapi.config.AppConfig

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class NotificationController @Inject() (
  authAction: AuthAction,
  cc: ControllerComponents,
  httpClientV2: HttpClientV2,
  appConfig: AppConfig,
  implicit val ec: ExecutionContext
) extends BackendController(cc) {

  def postNotification(): Action[AnyContent] = authAction.async { implicit request =>
    def call = httpClientV2
      .post(new URI(s"${appConfig.hipBaseUrl}/person/:nino/liability/universal-credit${
          if request.headers.get("terminate").contains("true") then "/termination" else ""
        }").toURL)
      .setHeader(
        "correlationId"        -> request.headers.get("correlationId").getOrElse(""),
        "gov-uk-originator-id" -> request.headers.get("gov-uk-originator-id").getOrElse("")
      )
      .withBody[JsValue](request.body.asJson.get)
      .execute[HttpResponse]()

    call.map(result =>
      Status(result.status)(result.body)
        .withHeaders(result.headers.toSeq.map((k, v) => (k, v.mkString(" "))): _*)
    )
  }
}
