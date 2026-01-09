/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.universalcreditliabilityapi.filters

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.universalcreditliabilityapi.utils.ApplicationConstants.HeaderNames.CorrelationId

import scala.concurrent.{ExecutionContext, Future}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestData.correlationId

class CorrelationIdFilterSpec extends AnyWordSpec with Matchers {

  given actorSystem: ActorSystem = ActorSystem("test-system")
  given materializer: Materializer = SystemMaterializer(actorSystem).materializer
  given ec: ExecutionContext = actorSystem.dispatcher

    val filter: CorrelationIdFilter = new CorrelationIdFilter()

    val next: RequestHeader => Future[Result] =
      _ => Future.successful(Results.Ok)

  "CorrelationIdFilter" should {

    "add the CorrelationId header when present in the request" in {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(CorrelationId -> correlationId)

      val futureResult: Future[Result] = filter.apply(next)(request)
      header(CorrelationId, futureResult) mustBe Some(correlationId)
    }

    "not add the CorrelationId header when missing from the request" in {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val futureResult: Future[Result] = filter.apply(next)(request)
      header(CorrelationId, futureResult) mustBe None
    }
  }
}
