package com.feedzai.cosytest.core

import java.nio.file.{Files, Paths}

import com.feedzai.cosytest.utils.FileTools
import com.feedzai.cosytest.{CleanUp, Utils}
import org.scalatest.{FlatSpec, MustMatchers}

import scala.io.Source

class ContainerFileCopySpec extends FlatSpec with MustMatchers with CleanUp {

  val setup = DockerComposeSetup(
    Utils.randomSetupName,
    Seq(Paths.get("src", "test", "resources", "docker-compose.yml")),
    Paths.get("").toAbsolutePath,
    Map.empty
  )

  override def dockerSetups = Seq(setup)

  it should "Returns false if an exception is thrown while trying to handle non-existing files" in {
    setup.dockerComposeUp()
    val containerId = setup.getProjectContainerIds().head

    setup.copyToContainer(containerId, Paths.get("nonExistingFile"), Paths.get("/opt")) mustBe false
    setup.dockerComposeDown()
  }

  it should "Retrieve a file from a container that was previously copied into it" in {
    setup.dockerComposeUp()
    val containerId = setup.getProjectContainerIds().head

    val tempFile = FileTools.createFile(Paths.get("newFile.txt"), List("some contents")).get
    setup.copyToContainer(containerId, tempFile, Paths.get("/opt")) mustBe true

    val copyFileDestination = Paths.get("/tmp/"+tempFile.getFileName.toString)
    setup.copyToHost(containerId, Paths.get("/opt/",tempFile.getFileName.toString), copyFileDestination) mustBe true

    Source.fromFile(copyFileDestination.toFile).mkString mustEqual Source.fromFile(tempFile.toFile).mkString

    Files.delete(tempFile)
    Files.delete(copyFileDestination)

    setup.dockerComposeDown()
  }

}
