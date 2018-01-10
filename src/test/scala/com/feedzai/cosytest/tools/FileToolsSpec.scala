package com.feedzai.cosytest.tools

import java.nio.file.{Files, Path, Paths}

import com.feedzai.cosytest.FileTools
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}

import collection.JavaConverters._

class FileToolsSpec extends FlatSpec with MustMatchers with BeforeAndAfterAll {

  val invalidFilePath: Path = Paths.get("invalidPath","newFile.txt")
  val validFilePath:   Path = Paths.get("newFile.txt")
  val validZipFile:    Path = Paths.get("newZipFile.zip")
  val invalidZipFile:  Path = Paths.get("invalid","newZipFile.zip")

  behavior of "Create File Method"

  it should "fail to create file for invalid path" in {
    val content = List("Dummy", "Lines", "End")
    val result = FileTools.createFile(invalidFilePath, content)

    result.isFailure mustEqual true
  }

  it should "create a file with success" in {
    val content = List("Dummy", "Lines", "End")
    val result = FileTools.createFile(validFilePath, content)

    result.isSuccess mustBe true
    Files.readAllLines(validFilePath).asScala.toList mustEqual content
    Files.delete(validFilePath)
  }

  it should "overwrite content if file already exists" in {
    val content = List("Dummy", "Lines", "End")
    val result = FileTools.createFile(validFilePath, content)

    result.isSuccess mustBe true
    Files.readAllLines(validFilePath).asScala.toList mustEqual content

    val newContent = List("New", "Content")
    val newResult = FileTools.createFile(validFilePath, newContent)

    newResult.isSuccess mustBe true
    Files.readAllLines(validFilePath).asScala.toList mustEqual newContent ++ List(content.last)
    Files.delete(validFilePath)
  }


  behavior of "Zip Method"

  it should "fail to zip non existent files" in {
    val result = FileTools.zip(validZipFile, Seq(Paths.get("invalid","file.txt")))

    result.isFailure mustBe true
    Files.delete(validZipFile)
  }

  it should "fail when using an invalid zip path" in {
    FileTools.createFile(validFilePath, List("Dummy", "File"))
    val result = FileTools.zip(invalidZipFile, Seq(validFilePath))

    result.isFailure mustBe true
    Files.delete(validFilePath)
  }

  it should "zip files" in {
    val validZipFile = Paths.get("newZipFile.zip")
    FileTools.createFile(validFilePath, List("Dummy", "File"))
    val result = FileTools.zip(validZipFile, Seq(validFilePath))

    result.isSuccess mustBe true
    Files.exists(validFilePath) mustBe false
    Files.exists(validZipFile) mustBe true

    Files.delete(validZipFile)
  }


  protected override def afterAll(): Unit = {
    super.afterAll()
    Files.deleteIfExists(invalidFilePath)
    Files.deleteIfExists(validFilePath)
    Files.deleteIfExists(invalidZipFile)
    Files.deleteIfExists(validZipFile)
  }
}
