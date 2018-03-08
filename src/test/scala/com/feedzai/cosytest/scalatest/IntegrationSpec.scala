package com.feedzai.cosytest.scalatest

import java.nio.file.Paths

import com.feedzai.cosytest.core.DockerComposeSetup
import org.scalatest.{FlatSpec, MustMatchers}

class IntegrationSpec extends FlatSpec with DockerComposeTestSuite with MustMatchers {

  override def dockerSetup = Some(
    DockerComposeSetup(
      "scalatest",
      Seq(Paths.get("src", "test", "resources", "docker-compose-scalatest.yml")),
      Paths.get("").toAbsolutePath,
      Map.empty
    )
  )

  behavior of "Scala Test"

  it must "Retrieve all services" in {
    val expectedServices = Set("container1", "container2", "container3")
    dockerSetup.foreach { setup =>
      setup.getServices().toSet mustEqual expectedServices
    }
  }

  it must "Retrieve correct mapped ports for all services" in {
    dockerSetup.foreach { setup =>
      val service1  = setup.getServiceMappedPort("container1", 80)
      service1.size mustBe 1
      service1.head mustEqual "8081"
      val service2  = setup.getServiceMappedPort("container2", 80)
      service2.size mustBe 1
      service2.head mustEqual "8082"
      val service3  = setup.getServiceMappedPort("container3", 80)
      service3.size mustBe 1
      service3.head mustEqual "8083"
    }
  }
}
