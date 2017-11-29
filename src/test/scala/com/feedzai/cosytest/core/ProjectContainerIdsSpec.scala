package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

class ProjectContainerIdsSpec extends FlatSpec with MustMatchers with CleanUp {

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

  it should "Return an empty list of ids when project doesn't exist" in {
    invalidSetup.getProjectContainerIds() mustEqual Seq.empty
  }

  it should "Return an empty list of ids when no containers exist" in {
    setup.getProjectContainerIds() mustEqual Seq.empty
  }

  it should "Return the service list of ids of all services" in {
    setup.dockerComposeUp()
    setup.getProjectContainerIds().size mustEqual 3
    setup.getProjectContainerIds().forall(_.isEmpty) must not be true
    setup.dockerComposeDown()
  }

}
