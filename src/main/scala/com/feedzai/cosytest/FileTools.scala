package com.feedzai.cosytest

import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.collection.JavaConverters._
import scala.util.Try

object FileTools {

  /**
   * Compresses files to zip file and deletes files.
   *
   * @param filePath file path including file name and directory where it will be created
   * @param files list of files path
   */
  def zip(filePath: Path, files: Seq[Path]): Try[Unit] = {
    Try {
      val zip = new ZipOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE))

      files.foreach { file =>
        zip.putNextEntry(new ZipEntry(file.toString))
        Files.copy(file, zip)
        Files.delete(file)
        zip.closeEntry()
      }
      zip.close()
    }
  }

  /**
   * Creates a file, in filePath with the given content.
   *
   * @param filePath path with file name
   * @param content list of strings that will be written in file
   * @return a Try object
   */
  def createFile(filePath: Path, content: List[String]): Try[Path] = {
    Try {
      Files.write(filePath, content.asJava, StandardOpenOption.CREATE)
    }
  }
}
