package com.feedzai.cosytest.core

import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

import scala.concurrent.duration._
import scala.util.Random

class ContainerIpSpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  override def dockerSetups = Seq(setup)

  it should "Return None when container doesn't exist" in {
    setup.getContainerAddress("h64387g") mustBe None
  }

  it should "Return None for invalid container" in {
    setup.up(2.minute)
    val idList = setup.getServiceContainerIds("container1")
    idList.size mustEqual 1
    idList.head.nonEmpty mustEqual true
    val invalidId = Random.alphanumeric.take(idList.head.length()).mkString.toLowerCase
    setup.getContainerAddress(invalidId) mustBe None
    setup.down()
  }

  it should "Return an IP for a valid container" in {
    setup.up(2.minute)
    val idList = setup.getServiceContainerIds("container1")
    idList.size mustEqual 1
    idList.head.nonEmpty mustEqual true
    setup.getContainerAddress(idList.head).isDefined mustEqual true
    setup.getContainerAddress(idList.head).get.isSiteLocalAddress mustEqual true
    setup.down()
  }

}
