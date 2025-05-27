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

package uk.gov.hmrc.universalcreditliabilityapi.platform.controllers

import controllers.Assets
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.universalcreditliabilityapi.config.AppConfig
import uk.gov.hmrc.universalcreditliabilityapi.platform.models.*

import javax.inject.{Inject, Singleton}
import scala.io.Source

@Singleton
class DocumentationController @Inject() (assets: Assets, cc: ControllerComponents, appConfig: AppConfig)
    extends BackendController(cc) {

  def definition(): Action[AnyContent] = Action {
    val apiBase  = Json.parse(Source.fromResource("api.definition/api-base.json").mkString(""))
    val versions = Json
      .parse(Source.fromResource("api.definition/default-versions.json").mkString(""))
      .validate[List[Version]]
      .get

    val updatedVersions = versions.map { versionObj =>
      val version              = versionObj.version
      val maybeConfigOverrides = appConfig.definitionOverrides.get(version)
      val status               = maybeConfigOverrides.flatMap(_.status).getOrElse(versionObj.status)
      val endpointsEnabled     = maybeConfigOverrides.map(_.endpointsEnabled).getOrElse(versionObj.endpointsEnabled)

      versionObj.copy(
        status = status,
        endpointsEnabled = endpointsEnabled
      )
    }

    val api = apiBase.as[JsObject] + ("versions" -> Json.toJson(updatedVersions))

    Ok(Json.obj("api" -> api))
  }

  def specification(version: String, file: String): Action[AnyContent] =
    assets.at(s"/public/api/conf/$version", file)
}
