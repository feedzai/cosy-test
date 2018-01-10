package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

class ServicesSpec extends FlatSpec with MustMatchers with CleanUp {

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

  it should "Return an empty list of services when project doesn't exist" in {
    invalidSetup.getServices() mustEqual Seq.empty
  }

  it should "Return the services name list when no containers exist" in {
    setup.getServices().size mustEqual 3
    setup.getServices().contains("container1") mustEqual true
    setup.getServices().contains("container2") mustEqual true
    setup.getServices().contains("container3") mustEqual true
  }

  it should "Return the services name list" in {
    setup.dockerComposeUp()
    setup.getServices().size mustEqual 3
    setup.getServices().contains("container1") mustEqual true
    setup.getServices().contains("container2") mustEqual true
    setup.getServices().contains("container3") mustEqual true
    setup.dockerComposeDown()
  }

}
