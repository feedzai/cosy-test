package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

class ProjectNetworksSpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  val invalidSetup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose_invalid.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  override def dockerSetups = Seq(setup, invalidSetup)

  it should "Return an empty list of network ids when project doesn't exist" in {
    invalidSetup.getProjectNetworkIds() mustEqual Seq.empty
  }

  it should "Return an empty list of network ids when no containers exist" in {
    setup.getProjectNetworkIds() mustEqual Seq.empty
  }

  it should "Return the network ids list of all services" in {
    setup.dockerComposeUp()
    setup.getProjectNetworkIds().size mustEqual 1
    setup.getProjectNetworkIds().nonEmpty mustBe true
    setup.dockerComposeDown()
  }
}
