package com.feedzai.cosytest.core

import java.io.{File}
import java.nio.file.{Paths}

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, FileTools, Utils}
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

    val tempFile = FileTools.createFile(Paths.get("newFile.txt"), List("some contents"))
    setup.copyToContainer(containerId, tempFile.get, Paths.get("/opt")) mustBe true

    val copyFileDestination = Paths.get(tempFile.get.getFileName.toString)
    setup.copyToHost(containerId, copyFileDestination, Paths.get("/opt/",tempFile.get.getFileName.toString)) mustBe true

    val copiedFile = new File(copyFileDestination.toUri)
    Source.fromFile(copyFileDestination.toFile).mkString mustEqual Source.fromFile(tempFile.get.toFile).mkString

    tempFile.get.toFile.delete()
    copiedFile.delete()
    setup.dockerComposeDown()
  }

}
