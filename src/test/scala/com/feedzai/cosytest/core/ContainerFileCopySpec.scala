package com.feedzai.cosytest.core

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
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

    setup.copyToContainer(containerId, "nonExistingFile", "/opt") mustBe false
    setup.dockerComposeDown()
  }

  it should "Retrieve a file from a container that was previously copied into it" in {
    setup.dockerComposeUp()
    val fileContent = "some content"
    val tempFile = File.createTempFile("tmp",".txt")
    new PrintWriter(tempFile) { write(fileContent); close() }
    val containerId = setup.getProjectContainerIds().head

    setup.copyToContainer(containerId, tempFile.getAbsolutePath, "/opt") mustBe true

    val copyFileDestination = ".";
    setup.copyToHost(containerId, copyFileDestination, "/opt/"+tempFile.getName) mustBe true

    val copiedFile = new File(copyFileDestination+"/"+tempFile.getName)

    Source.fromFile(copiedFile).mkString mustEqual fileContent

    tempFile.delete()
    copiedFile.delete()
    setup.dockerComposeDown()
  }

}
