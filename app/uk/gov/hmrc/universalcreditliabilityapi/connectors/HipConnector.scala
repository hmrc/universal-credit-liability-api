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

package uk.gov.hmrc.universalcreditliabilityapi.connectors

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.universalcreditliabilityapi.config.AppConfig
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, TerminateLiabilityRequest}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.{Authorization, CorrelationId, OriginatorId}

import java.util.Base64
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (httpClientV2: HttpClientV2, appConfig: AppConfig)(implicit val ec: ExecutionContext) {

  def sendUcLiability(
    nationalInsuranceNumber: String,
    correlationId: String,
    originatorId: String,
    requestObject: InsertLiabilityRequest | TerminateLiabilityRequest
  )(using hc: HeaderCarrier): Future[HttpResponse] = {
    val (url, requestBody) = requestObject match {
      case insert: InsertLiabilityRequest       =>
        (
          url"${appConfig.hipBaseUrl}/ni/person/$nationalInsuranceNumber/liability/universal-credit",
          Json.toJson(insert)
        )
      case terminate: TerminateLiabilityRequest =>
        (
          url"${appConfig.hipBaseUrl}/ni/person/$nationalInsuranceNumber/liability/universal-credit/termination",
          Json.toJson(terminate)
        )
    }

    httpClientV2
      .post(url)
      .setHeader(
        Authorization -> basicAuth,
        CorrelationId -> correlationId,
        OriginatorId  -> originatorId
      )
      .withBody(requestBody)
      .execute[HttpResponse]()
  }

  private def basicAuth: String = {
    val credentials: String = s"${appConfig.hipClientId}:${appConfig.hipClientSecret}"
    val encoded: String     = Base64.getEncoder.encodeToString(credentials.getBytes("UTF-8"))
    s"Basic $encoded"
  }

}
