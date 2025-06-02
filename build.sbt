import play.japi.twirl.compiler.TwirlCompiler
import uk.gov.hmrc.DefaultBuildSettings

import scala.collection.JavaConverters

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.5"

lazy val configTest = settingKey[String]("example")

configTest := TwirlKeys.templateImports.value.mkString("\n")

lazy val microservice = Project("universal-credit-liability-api", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(CodeCoverageSettings.settings*)
  .settings(
    PlayKeys.playDefaultPort := 16107,
    TwirlKeys.templateFormats += ("jso" -> "views.JsoFormat"),
    // remove unwanted twirl imports by resetting it to the bare minimum required
//    TwirlKeys.templateImports := JavaConverters.asScalaIterator(TwirlCompiler.DEFAULT_IMPORTS.iterator()).toSeq,
// remove the normal formatters
    //    TwirlKeys.templateImports -= "views.%format%._",
    TwirlKeys.templateImports += "uk.gov.hmrc.universalcreditliabilityapi.config.*"
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
