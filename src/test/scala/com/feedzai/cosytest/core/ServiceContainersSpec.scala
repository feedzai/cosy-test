package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, Utils}
import org.scalatest.{FlatSpec, MustMatchers}
import scala.concurrent.duration._

class ServiceContainersSpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  override def dockerSetups = Seq(setup)

  it should "Return an empty list of ids when no containers exist" in {
    setup.getServiceContainerIds("container1") mustEqual Seq.empty
  }

  it should "Return an empty list of ids for invalid service" in {
    setup.dockerComposeUp()
    setup.getServiceContainerIds("InvalidService") mustEqual Seq.empty
    setup.dockerComposeDown()
  }

  it should "Return the service list of ids" in {
    setup.dockerComposeUp()
    setup.getServiceContainerIds("container1").size mustEqual 1
    setup.getServiceContainerIds("container1").head.isEmpty must not be true
    setup.dockerComposeDown()
  }

  it should "Return an empty list of IPs when no containers exist" in {
    setup.getServiceContainerAddresses("container1") mustEqual Seq.empty
  }

  it should "Return an empty list of IPs for invalid service" in {
    setup.dockerComposeUp()
    setup.getServiceContainerAddresses("InvalidService") mustEqual Seq.empty
    setup.dockerComposeDown()
  }

  it should "Return the service list of ips" in {
    setup.up(2.minute)
    setup.getServiceContainerAddresses("container1").size mustEqual 1
    setup.getServiceContainerAddresses("container1").head.isSiteLocalAddress mustBe true
    setup.down()
  }

}
