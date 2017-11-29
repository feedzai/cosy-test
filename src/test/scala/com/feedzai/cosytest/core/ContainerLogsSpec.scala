package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup}
import org.scalatest.{FlatSpec, MustMatchers}

class ContainerLogsSpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    "healthy",
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  val invalidSetup = DockerComposeSetup(
    "invalid",
    Seq(Paths.get("src", "test", "resources", "docker-compose_invalid.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  it should "Return an empty list when project doesn't exist" in {
    invalidSetup.getContainerLogs(None) mustEqual Seq.empty
  }

  it should "Return a list when services are running" in {
    setup.dockerComposeUp()
    setup.getContainerLogs(Some("container1")).size must not be 0
    setup.getContainerLogs(None).size must not be 0
    setup.dockerComposeDown()
  }

}
