package com.feedzai.cosytest.gatling

import java.nio.file.Paths

import com.feedzai.cosytest.core.DockerComposeSetup
import io.gatling.http.Predef._
import io.gatling.core.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class IntegrationSpec extends DockerComposeSimulation {

  override def dockerSetup = Some(
    DockerComposeSetup(
      "gatling",
      Seq(Paths.get("src", "test", "resources", "docker-compose-gatling.yml")),
      Paths.get("").toAbsolutePath,
      Map.empty
    )
  )

  beforeSimulation()

  private val HttpProtocol: HttpProtocolBuilder = http
    .baseURL("http://localhost:8086")

  private val populationBuilder = {
    val getAction = http("Gatling simulation").get("")
    val s = scenario("Gatling simulation")
    s.during(10.seconds)(feed(Iterator.empty).exec(getAction))
      .inject(rampUsers(10).over(1.second))
      .protocols(HttpProtocol)
  }

  setUp(populationBuilder)
}
