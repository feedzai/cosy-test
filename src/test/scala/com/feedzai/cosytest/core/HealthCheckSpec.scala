package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.DockerComposeSetup
import org.scalatest.{FlatSpec, MustMatchers}
import scala.concurrent.duration._

class HealthCheckSpec extends FlatSpec with MustMatchers {


  val setup = DockerComposeSetup(
    "healthy",
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  val unhealthySetup = DockerComposeSetup(
    "unhealthy",
    Seq(Paths.get("src", "test", "resources", "docker-compose-unhealthy.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  it should "Succeed to wait for no existent containers" in {
    setup.waitForAllHealthyContainers(5.seconds) mustBe true
  }

  it should "Succeed to wait for all containers be in healthy state" in {
    setup.dockerComposeUp()   mustEqual true
    setup.waitForAllHealthyContainers(20.seconds) mustBe true
    setup.dockerComposeDown() mustEqual true
  }

  it should "Fail to wait for all containers be in healthy state" in {
    unhealthySetup.dockerComposeUp()   mustEqual true
    unhealthySetup.waitForAllHealthyContainers(20.seconds) mustBe false
    unhealthySetup.dockerComposeDown() mustEqual true
  }
}
