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

package views

import play.api.http.ContentTypeOf
import play.api.mvc.Codec
import play.twirl.api.utils.StringEscapeUtils
import play.twirl.api.{BufferedContent, Format}

class Jso(elements: Seq[Jso], text: String) extends BufferedContent[Jso](elements, text) {
  override val contentType = Jso.contentType
}

object Jso {
  val contentType = "application/json"

  implicit def contentTypeJso(implicit codec: Codec): ContentTypeOf[Jso] = ContentTypeOf[Jso](Some(Jso.contentType))

  def apply(text: String) = new Jso(Nil, Formats.safe(text))

  def apply(elements: Seq[Jso]) = new Jso(elements, "")

}

object Formats {
  def safe(text: String): String = if (text eq null) "" else text
}

object JsoFormat extends Format[Jso] {
  def raw(text: String): Jso =
    Jso(text)

  def escape(text: String): Jso =
    Jso(StringEscapeUtils.escapeEcmaScript(text))

  override def empty: Jso = Jso("")

  override def fill(elements: Seq[Jso]): Jso = Jso(elements)

}
