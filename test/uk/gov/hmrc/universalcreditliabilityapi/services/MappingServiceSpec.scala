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

package uk.gov.hmrc.universalcreditliabilityapi.services

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.universalcreditliabilityapi.helpers.TestData.{baseInsertDwpRequest, baseInsertHipRequest, baseTerminateDwpRequest, baseTerminateHipRequest, errorCodes}
import uk.gov.hmrc.universalcreditliabilityapi.models.dwp.response.Failure as DwpFailure
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.request.{InsertLiabilityRequest, TerminateLiabilityRequest}
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.Failure as HipFailure
import uk.gov.hmrc.universalcreditliabilityapi.models.hip.response.Failures as HipFailures

class MappingServiceSpec extends AnyWordSpec with Matchers {

  val testMappingService: MappingService = new MappingService()

  "MappingService.mapRequest" must {
    "map a DWP request to a HIP request" when {
      "given a valid 'Insert' request" in {
        val (nino, hipInsertRequest) = testMappingService.mapRequest(baseInsertDwpRequest)

        nino mustBe baseInsertDwpRequest.nationalInsuranceNumber
        hipInsertRequest mustBe baseInsertHipRequest
        hipInsertRequest mustBe a[InsertLiabilityRequest]
      }

      "given a valid 'Terminate' request" in {
        val (nino, hipTerminateRequest) = testMappingService.mapRequest(baseTerminateDwpRequest)

        nino mustBe baseTerminateDwpRequest.nationalInsuranceNumber
        hipTerminateRequest mustBe baseTerminateHipRequest
        hipTerminateRequest mustBe a[TerminateLiabilityRequest]
      }
    }
  }

  "MappingService.map422ResponseErrors" must {
    "map a HIP '422' failure to a single DWP '422' failure" when {
      for ((errorCode, errorMessage) <- errorCodes)
        s"HIP returns a 422 error with code '$errorCode'" in {
          val hipFailures = HipFailures(Seq(HipFailure(errorCode, errorMessage)))
          val result      = testMappingService.map422ResponseErrors(hipFailures)

          result mustBe Some(DwpFailure(errorCode, errorMessage))
        }
    }

    "map multiple HIP '422' failures to a single DWP '422' failure" when {
      "given a list of multiple HIP failures" in {
        val randomErrors                = scala.util.Random.shuffle(errorCodes).take(3)
        val (errorCode1, errorMessage1) = randomErrors(0)
        val (errorCode2, errorMessage2) = randomErrors(1)
        val (errorCode3, errorMessage3) = randomErrors(2)

        val hipFailures: HipFailures = HipFailures(
          Seq(
            HipFailure(errorCode1, errorMessage1),
            HipFailure(errorCode2, errorMessage2),
            HipFailure(errorCode3, errorMessage3)
          )
        )

        val result = testMappingService.map422ResponseErrors(hipFailures)

        result mustBe Some(DwpFailure(errorCode1, errorMessage1))
      }
    }

    "return None" when {
      "given an empty HIP failures list" in {
        testMappingService.map422ResponseErrors(HipFailures(Seq.empty[HipFailure])) mustBe None
      }
    }

  }
}
