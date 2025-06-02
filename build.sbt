import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.5"

libraryDependencies += guice

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
    TwirlKeys.templateImports += "uk.gov.hmrc.universalcreditliabilityapi.config.*",
    TwirlKeys.templateFormats += ("jso" -> "views.JsoFormat")
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
