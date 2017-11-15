package com.feedzai.cosytest

import java.io.File
import java.nio.file.{Path, Paths}

import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Success, Try}

case class DockerComposeSetup(
  setupName: String,
  composeFiles: Seq[Path],
  workingDirectory: Path,
  environment: Map[String, String]
) {
  private val logger = LoggerFactory.getLogger(getClass)
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private val DefaultLongCommandTimeOut  = 5.minutes
  private val DefaultShortCommandTimeOut = 1.minutes

  /**
   * Executes docker-compose up command using setup defined and waits for
   * all containers with healthchecks to be in a healthy state.
   *
   * @param timeOut maximum time for all containers be in a healthy state
   * @return true if all containers started and achieved a healthy state otherwise false
   */
  def up(timeOut: Duration): Boolean = dockerComposeUp() && waitForAllHealthyContainers(timeOut)


  /**
   * Executes docker-compose down command using setup defined.
   *
   * @return true if all containers were stopped and removed otherwise false
   */
  def down(): Boolean = dockerComposeDown() && checkContainersRemoval()

  /**
   * Retrieves port that was mapped to binded port for specified service.
   *
   * @param name service name
   * @param bindedPort port that was binded to some specific port
   * @return Returns a list of ports from all service containers that have binded port
   */
  def getServiceMappedPort(name: String, bindedPort: Int): Seq[String] = {
    getServiceContainerIds(name)
      .map(getContainerMappedPort(_, bindedPort))
  }

  /**
   * Dump logs from STDOUT using docker-compose logs for each service. Logs will be saved to
   * target dir in a zip file with the name specified.
   *
   * @param fileName zip file name
   * @param target directory where zip file will be saved
   * @return Try object that if fails contains exception, otherwise returns nothing
   */
  def dumpLogs(fileName: String, target: Path): Try[Unit] = {
    val services = getServices()

    val files = services.flatMap { service =>
      FileTools.createFile(
        Paths.get(s"log_$service"),
        getContainerLogs(Some(service))
      ).toOption
    }
    FileTools.zip(target.resolve(s"$fileName.zip").toAbsolutePath, files)
  }

  def getContainerMappedPort(containerId: String, port: Int): String = {
    val command = Seq(
      "docker",
      "port",
      containerId,
      port.toString
    )
    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) if output.nonEmpty => output.head.replaceAll("^.+:","")
      case _ => ""
    }
  }

  def getServiceContainerIds(serviceName: String): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "ps", "-q", serviceName)

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed to get service container Ids!",f)
        List.empty
    }
  }

  def getProjectContainerIds(): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "ps", "-q")

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed to get all project container Ids!",f)
        List.empty
    }
  }


  def getServices(): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "config", "--services")

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed to get all services!",f)
        List.empty
    }
  }

  def isContainerWithHealthCheck(containerId: String): Boolean = {
    val command = Seq(
      "docker",
      "inspect",
      containerId,
      "--format={{ .State.Health }}"
    )

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output.nonEmpty && output.size == 1 && output.head != "<nil>"
      case Failure(f) =>
        logger.error(s"Failed while checking if container $containerId contains healthcheck!",f)
        false
    }
  }

  def dockerComposeUp(): Boolean = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "up", "-d")

    runCmd(command, workingDirectory.toFile, environment, DefaultLongCommandTimeOut)
  }

  def dockerComposeDown(): Boolean = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "down")

    runCmd(command, workingDirectory.toFile, environment, DefaultLongCommandTimeOut)
  }

  def waitForHealthyContainer(containerId: String, timeout: Duration): Boolean = {
    val command = Seq(
      "docker",
      "inspect",
      containerId,
      "--format={{ .State.Health.Status }}"
    )

    val future = Future[Boolean] {
      var result = ""
      var done = false

      while (!done) {
        val eventBuilder = new StringBuilder()
        val process = Process(command).run(ProcessLogger(line => eventBuilder.append(line), line => logger.debug(line)))
        val exitValue = waitProcessExit(process, DefaultShortCommandTimeOut)
        done = exitValue == 0 && eventBuilder.nonEmpty && eventBuilder.mkString == "healthy"
        if (done) {
          result = eventBuilder.mkString
        }
        else {
          Thread.sleep(10.seconds.toMillis)
        }
      }
      result.nonEmpty
    }

    try {
      Await.result(future, timeout)
    } catch {
      case e: TimeoutException =>
        e.printStackTrace()
        false
    }
  }

  def waitForAllHealthyContainers(timeout: Duration): Boolean = {
    getProjectContainerIds()
      .filter(isContainerWithHealthCheck)
      .forall(waitForHealthyContainer(_, timeout))
  }


  def getContainerLogs(serviceName: Option[String]): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "logs", "--no-color") ++
        serviceName

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed while retrieving logs of ${serviceName.get}",f)
        List.empty
    }
  }

  def checkContainersRemoval(): Boolean = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", setupName, "ps", "-q")

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output.isEmpty
      case Failure(f) =>
        logger.error("Failed while checking containers removal",f)
        false
    }
  }

  private def waitProcessExit(process: Process, timeout: Duration): Int = {
    val future = Future(blocking(process.exitValue()))
    try {
      Await.result(future, timeout)
    } catch {
      case e: TimeoutException =>
        e.printStackTrace()
        process.destroy()
        process.exitValue()
    }
  }

  /**
   * Runs a command using specified working directory.
   *
   * @param command list of strings that compose the command
   * @param cwd working directory where command will be ran
   * @return a Try object with output lines list if succeeds
   */
  def runCmdWithOutput(command: Seq[String], cwd: File, envVars: Map[String, String],  timeout: Duration): Try[List[String]] = {
    Try {
      val resultBuffer = new ListBuffer[String]()
      val process =
        Process(command, cwd, envVars.toSeq: _*).run(ProcessLogger(line => resultBuffer += line, line => logger.debug(line)))

      assert(waitProcessExit(process, timeout) == 0, s"Failed to run command: ${command.mkString(" ")}")
      resultBuffer.toList
    }
  }

  /**
   * Runs a command using specified working directory.
   *
   * @param command list of strings that compose the command
   * @param cwd working directory where command will be ran
   * @return true if command ran with success
   */
  def runCmd(command: Seq[String], cwd: File, envVars: Map[String, String],  timeout: Duration): Boolean = {
    val process =
      Process(command, cwd, envVars.toSeq: _*).run(ProcessLogger(_ => (), line => logger.debug(line)))

    waitProcessExit(process, timeout) == 0
  }

  private def composeFileArguments(files: Seq[Path]): Seq[String] = {
    files.flatMap(file => Seq("-f",file.toString))
  }
}
