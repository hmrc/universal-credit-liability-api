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

package uk.gov.hmrc.universalcreditliabilityapi

import org.scalactic.source.Position
import org.scalatest.{ConfigMap, Outcome, TestData}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerTest
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse, readableAsJson}

class DefinitionEndpointIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerTest
    with ScalaFutures
    with IntegrationPatience {

  private def wsClient = app.injector.instanceOf[WSClient]
  private def baseUrl  = s"http://localhost:$port"

  override def newAppForTest(testData: TestData): Application =
    GuiceApplicationBuilder()
      .configure(testData.configMap)
      .build()

  override def withFixture(test: NoArgTest): Outcome = {
    val config: NoArgTest =
      test.name match {
        case name if name.contains("[noOverride]")   =>
          test
        case name if name.contains("[withOverride]") =>
          test.copy(
            ConfigMap(
              "apiPlatformStatus"           -> "BETA",
              "apiPlatformEndpointsEnabled" -> "true"
            )
          )
        case _                                       =>
          throw Error(s"Unconfigured testCase: ${test.name}")
      }

    super.withFixture(config)
  }

  private def callGetApiDefinition(): WSResponse = wsClient
    .url(s"$baseUrl/api/definition")
    .get()
    .futureValue

  "GET /api/definition" must {
    "[noOverride] respond with 200 status with default definition when no configs are set" in {
      val response = callGetApiDefinition()

      response.status mustBe Status.OK
      response.body[JsValue] mustBe Expected.definitionJson("ALPHA", false)
    }

    "[withOverride] respond with 200 status with overridden definition when the corresponding configs are set" in {
      val response = callGetApiDefinition()

      response.status mustBe Status.OK
      response.body[JsValue] mustBe Expected.definitionJson("BETA", true)
    }
  }

  extension (test: NoArgTest) {
    def copy(_configMap: ConfigMap): NoArgTest =
      new NoArgTest {
        val name: String         = test.name
        val configMap: ConfigMap = _configMap

        def apply(): Outcome = test()

        val scopes: IndexedSeq[String] = test.scopes
        val text: String               = test.text
        val tags: Set[String]          = test.tags
        val pos: Option[Position]      = test.pos
      }
  }
}

object Expected {
  def definitionJson(status: String, endpointEnabled: Boolean): JsValue = Json.parse(
    s"""
       |{
       |  "api": {
       |    "name": "Universal Credit Liability",
       |    "description": "This API provides the capability to receive UC business data from DWP's Universal Credits Full Service (UCFS) system.",
       |    "context": "misc/universal-credit/liability",
       |    "categories": [
       |      "PRIVATE_GOVERNMENT"
       |    ],
       |    "versions": [
       |      {
       |        "version": "1.0",
       |        "status": "$status",
       |        "endpointsEnabled": $endpointEnabled
       |      }
       |    ]
       |  }
       |}
       |""".stripMargin
  )
}
