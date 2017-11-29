package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

class ContainerLogsSpec extends FlatSpec with MustMatchers with CleanUp {

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

  it should "Return an empty list when project doesn't exist" in {
    invalidSetup.getContainerLogs(None) mustEqual Seq.empty
  }

  it should "Return a list when services are running" in {
    setup.dockerComposeUp()
    val containerLogs = setup.getContainerLogs(Some("container1"))
    val allLogs = setup.getContainerLogs(None)
    containerLogs.size must not be 0
    containerLogs.exists(_.contains("container1_1")) mustBe true
    containerLogs.exists(_.contains("container2_1")) mustBe false
    containerLogs.exists(_.contains("container3_1")) mustBe false
    allLogs.size must not be 0
    allLogs.exists(_.contains("container1_1")) mustBe true
    allLogs.exists(_.contains("container2_1")) mustBe true
    allLogs.exists(_.contains("container3_1")) mustBe true
    setup.dockerComposeDown()
  }

}
