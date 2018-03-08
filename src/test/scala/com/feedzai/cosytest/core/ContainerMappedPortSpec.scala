package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

import scala.util.Random

class ContainerMappedPortSpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  override def dockerSetups = Seq(setup)

  it should "Return an empty string when no containers exist" in {
    setup.getContainerMappedPort("h64387g", 80) mustEqual ""
  }

  it should "Return an empty string for invalid service" in {
    setup.dockerComposeUp()
    val idList = setup.getServiceContainerIds("container1")
    idList.size mustEqual 1
    idList.head.nonEmpty mustEqual true
    val invalidId = Random.alphanumeric.take(idList.head.length()).mkString.toLowerCase
    setup.getContainerMappedPort(invalidId, 80) mustEqual ""
    setup.dockerComposeDown()
  }

  it should "Return an empty string for invalid binded port" in {
    setup.dockerComposeUp()
    val idList = setup.getServiceContainerIds("container1")
    idList.size mustEqual 1
    idList.head.nonEmpty mustEqual true
    setup.getContainerMappedPort(idList.head, 4000) mustEqual ""
    setup.dockerComposeDown()
  }

  it should "Return the service port mapped to binded port" in {
    setup.dockerComposeUp()
    val idList = setup.getServiceContainerIds("container1")
    idList.size mustEqual 1
    idList.head.nonEmpty mustEqual true
    setup.getContainerMappedPort(idList.head, 80).forall(_.isDigit) mustEqual true
    setup.dockerComposeDown()
  }

}
