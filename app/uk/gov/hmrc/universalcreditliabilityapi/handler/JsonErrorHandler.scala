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

package uk.gov.hmrc.universalcreditliabilityapi.handler

import play.api.Configuration
import play.api.mvc.Results.InternalServerError
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.http.JsonErrorHandler as DefaultJsonErrorHandler
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.CorrelationId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JsonErrorHandler @Inject() (
  auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends DefaultJsonErrorHandler(auditConnector, httpAuditEvent, configuration) {

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] =
    super
      .onServerError(request, ex)
      .map(_ =>
        request.headers
          .get(CorrelationId)
          .map(id => InternalServerError.withHeaders(CorrelationId -> id))
          .getOrElse(InternalServerError)
      )
}
