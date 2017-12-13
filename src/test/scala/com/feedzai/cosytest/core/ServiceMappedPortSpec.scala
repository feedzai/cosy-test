package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

class ServiceMappedPortSpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  override def dockerSetups = Seq(setup)

  it should "Return an empty list of ports when no containers exist" in {
    setup.getServiceMappedPort("container1", 80) mustEqual Seq.empty
  }

  it should "Return an empty list of ports for invalid service" in {
    setup.dockerComposeUp()
    setup.getServiceMappedPort("InvalidService", 80) mustEqual Seq.empty
    setup.dockerComposeDown()
  }

  it should "Return an empty list of ports for invalid binded port" in {
    setup.dockerComposeUp()
    setup.getServiceMappedPort("container1", 4000) mustEqual Seq.empty
    setup.dockerComposeDown()
  }

  it should "Return the service list of ports mapped to binded port" in {
    setup.dockerComposeUp()
    setup.getServiceMappedPort("container1", 80).size mustEqual 1
    setup.getServiceMappedPort("container1", 80).head.forall(_.isDigit) mustEqual true
    setup.dockerComposeDown()
  }

}
