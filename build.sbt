import sbt.Keys.dependencyOverrides
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"
ThisBuild / semanticdbEnabled := true

lazy val microservice = Project("universal-credit-liability-api", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(CodeCoverageSettings.settings *)
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(PlayKeys.playDefaultPort := 16107)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    DefaultBuildSettings.itSettings(),
    libraryDependencies ++= AppDependencies.it,
    // dependencyOverrides for:
    // "swagger-request-validator-core" % "2.44.8"
    // "com.github.tomakehurst" % "wiremock" % "3.0.1"
    // Scala module 2.15.3 requires Jackson Databind version >= 2.15.0 and < 2.16.0
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.25.0"
  )

addCommandAlias("runPrePullRequestChecks", "; scalafmtCheckAll; scalafmtSbtCheck; scalafixAll --check")
addCommandAlias("checkCodeCoverage", "; clean; coverage; test; it/test; coverageReport")
addCommandAlias("lintCode", "; scalafmtAll; scalafmtSbt; scalafixAll")
