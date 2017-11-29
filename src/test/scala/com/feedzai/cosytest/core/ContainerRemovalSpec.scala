package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

class ContainerRemovalSpec extends FlatSpec with MustMatchers with CleanUp {

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

  it should "Return false when project doesn't exist" in {
    invalidSetup.checkContainersRemoval() mustEqual false
  }

  it should "Return true when no containers are started" in {
    setup.checkContainersRemoval() mustEqual true
  }

  it should "Return values correctly when containers exist or not" in {
    setup.checkContainersRemoval() mustEqual true
    setup.dockerComposeUp()
    setup.checkContainersRemoval() mustEqual false
    setup.dockerComposeDown()
    setup.checkContainersRemoval() mustEqual true
  }

}
