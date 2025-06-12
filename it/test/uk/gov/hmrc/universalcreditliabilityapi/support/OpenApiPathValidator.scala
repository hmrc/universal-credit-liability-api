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

package uk.gov.hmrc.universalcreditliabilityapi.support

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.{SimpleRequest, SimpleResponse}
import com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE
import com.atlassian.oai.validator.report.{LevelResolver, ValidationReport}
import play.api.libs.ws.{EmptyBody, InMemoryBody, WSClient, WSRequest, WSResponse}
import play.api.libs.ws.DefaultBodyReadables.readableAsString
import com.atlassian.oai.validator.model.Request.Method
import org.scalatestplus.play.PortNumber
import scala.jdk.CollectionConverters._
import scala.io.Source

final class OpenApiValidator private (openapi: String) {

  private val validator: OpenApiInteractionValidator = OpenApiInteractionValidator
    .createForInlineApiSpecification(openapi)
    .withLevelResolver(
      // The key here is to use the level resolver to ignore the response validation messages
      // Without this they would be emitted at ERROR level and cause a validation failure.
      LevelResolver
        .create()
        .withLevel("validation.response", IGNORE)
        .build()
    )
    .build()

  def forPath(
    method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE" | "HEAD" | "OPTIONS" | "TRACE",
    context: String,
    path: String
  ): EndpointValidator =
    new EndpointValidator(validator)(Method.valueOf(method), context: String, path)

}

object OpenApiValidator {
  def fromResource(resource: String) = new OpenApiValidator(Source.fromResource(resource).mkString)
}

final case class ValidationError(message: String)

final class EndpointValidator private[support] (validator: OpenApiInteractionValidator)(
  method: Method,
  context: String,
  path: String
) {

  def newRequestBuilder(secure: Boolean = false)(using wsClient: WSClient, portNumber: PortNumber): WSRequest =
    wsClient.url((if (secure) "https" else "http") + "://localhost:" + portNumber.value + path).withMethod(method.name)

  def validateRequest(wsRequest: WSRequest): List[ValidationError] =
    (Method.valueOf(wsRequest.method), wsRequest.uri.getPath) match {
      case (`method`, `path`) =>
        validator
          .validateRequest(buildRequest(wsRequest))
          .getMessages
          .asScala
          .toList
          .map(msg => ValidationError.apply(msg.toString))
      case _                  =>
        List(
          ValidationError(
            s"The request ${wsRequest.method} ${wsRequest.uri.getPath} is not for the configured endpoint $method $path"
          )
        )
    }

  def validateResponse(wsResponse: WSResponse): List[ValidationError] =
    wsResponse.uri.getPath match {
      case `path` =>
        validator
          .validateResponse(
            context + path,
            method,
            buildResponse(wsResponse)
          )
          .getMessages
          .asScala
          .toList
          .map(msg => ValidationError.apply(msg.toString))
      case _      =>
        List(
          ValidationError(
            s"The response $method ${wsResponse.uri.getPath} is not for the configured endpoint $method $path"
          )
        )
    }

  private def buildRequest(wsRequest: WSRequest): SimpleRequest = {
    val initBuilder = SimpleRequest.Builder
      .post(context + wsRequest.uri.getPath)

    val builderWithHeaders = wsRequest.headers.toSeq
      .foldLeft(
        initBuilder
      ) { case (builder, (header, value)) =>
        builder.withHeader(header, value: _*)
      }

    val requestBodyAsString: String = wsRequest.body match {
      case InMemoryBody(bytes) => bytes.decodeString("utf-8")
      case EmptyBody           => ""
      case _                   => throw Error("Unsupported WsBody type")
    }

    val buildWithBody = builderWithHeaders.withBody(
      requestBodyAsString
    )

    buildWithBody.build()
  }

  private def buildResponse(wsResponse: WSResponse) = {
    val baseBuilder = SimpleResponse.Builder.status(wsResponse.status)

    val builderWithHeaders = wsResponse.headers.toSeq
      .foldLeft(
        baseBuilder
      ) { case (builder, (header, value)) =>
        builder.withHeader(header, value.toList.asJava)
      }

    builderWithHeaders.withBody(wsResponse.body[String]).build()
  }
}
