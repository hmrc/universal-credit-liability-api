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
import play.api.libs.json.{Json => PlayJson}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.universalcreditliabilityapi.config.AppConfig
import uk.gov.hmrc.universalcreditliabilityapi.views.*

import javax.inject.{Inject, Singleton}

@Singleton
class DocumentationController @Inject() (
  assets: Assets,
  cc: ControllerComponents,
  appConfig: AppConfig
) extends BackendController(cc) {

  def definition(): Action[AnyContent] = Action {
    Ok(
//      jso.definition(appConfig) // can't seem to get custom formatter working
      PlayJson.parse(js.definition(appConfig).toString) // javascript formatter also escapes the same thing

      // in both cases the template is not very well formatted due to lack of IDE support
    )
  }

  def specification(version: String, file: String): Action[AnyContent] =
    assets.at(s"/public/api/conf/$version", file)
}
