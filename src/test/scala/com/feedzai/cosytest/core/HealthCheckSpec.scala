package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup}
import org.scalatest.{FlatSpec, MustMatchers}
import scala.concurrent.duration._

class HealthCheckSpec extends FlatSpec with MustMatchers with CleanUp {

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

  /*
     Is container with health check
   */
  it should "Return false when container doesn't exist" in {
    unhealthySetup.isContainerWithHealthCheck("invalidId") mustBe false
  }

  it should "Return false when container doesn't have health check" in {
    unhealthySetup.dockerComposeUp() mustEqual true
    unhealthySetup.isContainerWithHealthCheck("container3") mustBe false
    unhealthySetup.dockerComposeDown() mustEqual true
  }

  it should "Return true when container have health check" in {
    unhealthySetup.dockerComposeUp() mustEqual true
    val id = unhealthySetup.getServiceContainerIds("container2")
    id.size mustBe 1
    unhealthySetup.isContainerWithHealthCheck(id.head) mustBe true
    unhealthySetup.dockerComposeDown() mustEqual true
  }

  /*
     Wait for healthy container
   */
  it should "Fail to wait for no existent container id" in {
    setup.waitForHealthyContainer("invalidId", 5.seconds) mustBe false
  }

  it should "Fail when container doesn't have health check" in {
    unhealthySetup.dockerComposeUp() mustEqual true
    val id = unhealthySetup.getServiceContainerIds("container3")
    id.size mustBe 1
    unhealthySetup.waitForHealthyContainer(id.head, 5.seconds) mustBe false
    unhealthySetup.dockerComposeDown() mustEqual true
  }

  it should "Fail when container is unhealthy" in {
    unhealthySetup.dockerComposeUp() mustEqual true
    val id = unhealthySetup.getServiceContainerIds("container2")
    id.size mustBe 1
    unhealthySetup.waitForHealthyContainer(id.head, 5.seconds) mustBe false
    unhealthySetup.dockerComposeDown() mustEqual true
  }

  it should "Succeed when container is healthy" in {
    setup.dockerComposeUp() mustEqual true
    val id = setup.getServiceContainerIds("container1")
    id.size mustBe 1
    setup.waitForHealthyContainer(id.head, 20.seconds) mustBe true
    setup.dockerComposeDown() mustEqual true
  }

  /*
     Wait for all healthy containers
   */
  it should "Succeed to wait for no existent containers" in {
    setup.waitForAllHealthyContainers(5.seconds) mustBe true
  }

  it should "Succeed to wait for all containers be in healthy state" in {
    setup.dockerComposeUp() mustEqual true
    setup.waitForAllHealthyContainers(20.seconds) mustBe true
    setup.dockerComposeDown() mustEqual true
  }

  it should "Fail to wait for all containers be in healthy state" in {
    unhealthySetup.dockerComposeUp() mustEqual true
    unhealthySetup.waitForAllHealthyContainers(20.seconds) mustBe false
    unhealthySetup.dockerComposeDown() mustEqual true
  }
}
