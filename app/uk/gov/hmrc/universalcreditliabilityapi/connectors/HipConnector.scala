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
import play.api.mvc.Request
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.universalcreditliabilityapi.config.AppConfig
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.hip.{InsertLiabilityRequest, TerminateLiabilityRequest}
import uk.gov.hmrc.universalcreditliabilityapi.models.requests.{InsertUcLiabilityRequest, TerminateUcLiabilityRequest}
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.{CorrelationId, OriginatorId}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (httpClientV2: HttpClientV2, appConfig: AppConfig)(implicit val ec: ExecutionContext) {

  def insertUcLiability(request: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {

    val apiInsertRequest = request.body.as[InsertUcLiabilityRequest]
    val hipRequest       = InsertLiabilityRequest(apiInsertRequest)

    val url = appConfig.hipBaseUrl

    httpClientV2
      .post(url"$url/person/${apiInsertRequest.nationalInsuranceNumber}/liability/universal-credit")
      .setHeader(
        CorrelationId -> request.headers.get(CorrelationId).getOrElse(""),
        OriginatorId  -> request.headers.get(OriginatorId).getOrElse("")
      )
      .withBody(Json.toJson(hipRequest))
      .execute[HttpResponse]()
  }

  def terminateUcLiability(request: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {

    val apiInsertRequest = request.body.as[TerminateUcLiabilityRequest]
    val hipRequest       = TerminateLiabilityRequest(apiInsertRequest)

    val url = appConfig.hipBaseUrl

    httpClientV2
      .post(url"$url/person/${apiInsertRequest.nationalInsuranceNumber}/liability/universal-credit/termination")
      .setHeader(
        CorrelationId -> request.headers.get(CorrelationId).getOrElse(""),
        OriginatorId  -> request.headers.get(OriginatorId).getOrElse("")
      )
      .withBody(Json.toJson(hipRequest))
      .execute[HttpResponse]()
  }

}
