import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                 % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatestplus" %% "mockito-5-18"           % "3.2.19.0"       % Test
  )

  val it: Seq[ModuleID] = Seq(
    "com.github.tomakehurst" % "wiremock"                       % "3.0.1"  % Test,
    "com.atlassian.oai"      % "swagger-request-validator-core" % "2.46.0" % Test
  )
}
