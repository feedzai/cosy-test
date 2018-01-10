package com.feedzai.cosytest.core

import java.nio.file.{Files, Path, Paths}
import java.util.zip.ZipFile

import com.feedzai.cosytest.{CleanUp, DockerComposeSetup, Utils}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}

class DumpLogsSpec extends FlatSpec with MustMatchers with BeforeAndAfterAll with CleanUp {

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

  val zipFile = "zipFile"
  val zipPath: Path = Paths.get(s"$zipFile.zip")

  override def dockerSetups = Seq(setup, invalidSetup)

  it should "Return a Failure when project doesn't exist" in {
    invalidSetup.dumpLogs(zipFile, Paths.get("")).isSuccess mustBe true
    val zip = new ZipFile(zipPath.toString)
    zip.size mustEqual 0
    Files.delete(zipPath)
  }

  it should "Succeed to zip log files" in {
    setup.dockerComposeUp()
    setup.dumpLogs(zipFile, Paths.get("")).isSuccess mustBe true
    setup.dockerComposeDown()

    val zip = new ZipFile(zipPath.toString)
    zip.size() mustEqual setup.getServices().size
    Files.delete(zipPath)
  }

  protected override def afterAll(): Unit = {
    super.afterAll()
    Files.deleteIfExists(zipPath)
  }
}
