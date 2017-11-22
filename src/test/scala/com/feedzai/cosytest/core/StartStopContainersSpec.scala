package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.DockerComposeSetup
import org.scalatest.{FlatSpec, MustMatchers}

class StartStopContainersSpec extends FlatSpec with MustMatchers {

  val setup = DockerComposeSetup(
    "startstop",
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  val invalidSetup = DockerComposeSetup(
    "invalidstartstop",
    Seq(Paths.get("src", "test", "resources", "docker-compose_invalid.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )


  it should "Start and stop containers" in {
    setup.dockerComposeUp()   mustEqual true
    setup.dockerComposeDown() mustEqual true
  }

  it must "Fail to start and stop containers" in {
    invalidSetup.dockerComposeUp()   mustEqual false
    invalidSetup.dockerComposeDown() mustEqual false
  }
}
