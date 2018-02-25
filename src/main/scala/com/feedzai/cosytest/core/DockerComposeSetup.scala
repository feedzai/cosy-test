package com.feedzai.cosytest.core

import java.io.File
import java.net.InetAddress
import java.nio.file.{Path, Paths}

import com.feedzai.cosytest.utils.FileTools
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Success, Try}

/**
 * The Docker Compose setup to be managed and to interact with.
 *
 * @param projectName      The Docker Compose project name.
 * @param composeFiles     The Docker Compose files used to build the setup environment.
 * @param workingDirectory The Docker Compose working directory
 * @param environment      The environment variables provided to the setup environment.
 */
case class DockerComposeSetup(
  projectName: String,
  composeFiles: Seq[Path],
  workingDirectory: Path,
  environment: Map[String, String]
) {
  private val logger = LoggerFactory.getLogger(getClass)
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private val DefaultLongCommandTimeOut = 5.minutes
  private val DefaultShortCommandTimeOut = 1.minutes

  /**
   * Executes docker-compose up command using setup defined and waits for
   * all containers with healthchecks to be in a healthy state.
   *
   * @param timeout maximum time for all containers be in a healthy state
   * @return true if all containers started and achieved a healthy state otherwise false
   */
  def up(timeout: Duration): Boolean = dockerComposeUp() && waitForAllHealthyContainers(timeout)

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
      .filter(getContainerMappedPort(_, bindedPort).nonEmpty)
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
      FileTools
        .createFile(
          Paths.get(s"log_$service"),
          getContainerLogs(Some(service))
        )
        .toOption
    }
    FileTools.zip(target.resolve(s"$fileName.zip").toAbsolutePath, files)
  }

  /**
    * Removes network and containers started by setup if still present.
    *
    * @return true if project network and containers were removed with success otherwise false
    */
  def cleanup(): Boolean = {
    val containerIds = getProjectContainerIds()

    val stoppedContainers = stopAllContainers(containerIds)
    val removedContainers = removeAllContainers(containerIds)
    val removedNetworks   = getProjectNetworkIds().forall(removeNetwork)

    if (!stoppedContainers) {
      logger.error("Failed to stop containers...")
    }

    if (!removedContainers) {
      logger.error("Failed to remove containers...")
    }

    if (!removedNetworks) {
      logger.error("Failed to remove networks...")
    }

    stoppedContainers && removedContainers && removedNetworks
  }

  /**
    * Returns the port open in localhost mapped to container port.
    *
    * @param containerId
    * @param port exposed from container
    * @return string of open localhost port. Empty if a failure occurs
    */
  def getContainerMappedPort(containerId: String, port: Int): String = {
    val command = Seq(
      "docker",
      "port",
      containerId,
      port.toString
    )
    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) if output.nonEmpty => output.head.replaceAll("^.+:", "")
      case _                                  => ""
    }
  }

  /**
    * Returns the list of container Ids associated to service.
    *
    * @param serviceName string
    * @return list of container ids. If a failure occurs an empty list is returned
    */
  def getServiceContainerIds(serviceName: String): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "ps", "-q", serviceName)

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed to get service container Ids!", f)
        List.empty
    }
  }

  /**
    * Returns the list of container IP Addresses associated to service.
    *
    * @param serviceName string
    * @return list of container ips.
    */
  def getServiceContainerAddresses(serviceName: String): List[InetAddress] = {
    getServiceContainerIds(serviceName).flatMap(id => getContainerAddress(id))
  }

  /**
    * Returns the list of container Ids associated to the project.
    *
    * @return list of container ids. If a failure occurs an empty list is returned.
    */
  def getProjectContainerIds(): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "ps", "-q")

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed to get all project container Ids!", f)
        List.empty
    }
  }

  /**
    * Returns the list of container IP Addresses associated to the project.
    *
    * @return list of container ips.
    */
  def getProjectContainerAddresses(): List[InetAddress] = {
    getProjectContainerIds().flatMap(id => getContainerAddress(id))
  }

  /**
    * Returns the list of network Ids associated to the project.
    *
    * @return list of network ids. If a failure occurs an empty list is returned.
    */
  def getProjectNetworkIds(): List[String] = {
    getAllNetworkIds().filter(isNetworkFromProject)
  }

  /**
    * Returns the list of services associated to the project.
    *
    * @return list of services. If a failure occurs an empty list is returned
    */
  def getServices(): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "config", "--services")

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        logger.error(s"Failed to get all services!", f)
        List.empty
    }
  }

  /**
    * Checks if container has an health check.
    *
    * @return true if an health check is found, otherwise false.
    */
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
        logger.error(s"Failed while checking if container $containerId contains healthcheck!", f)
        false
    }
  }

  /**
    * Runs `docker-compose up` command
    *
    * @return true if command has run with success, otherwise false.
    */
  def dockerComposeUp(): Boolean = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "up", "-d")

    runCmd(command, workingDirectory.toFile, environment, DefaultLongCommandTimeOut)
  }

  /**
    * Runs `docker-compose down` command
    *
    * @return true if command has run with success, otherwise false.
    */
  def dockerComposeDown(): Boolean = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "down")

    runCmd(command, workingDirectory.toFile, environment, DefaultLongCommandTimeOut)
  }

  /**
    * Waits for container to be in an healthy state within the timeout interval.
    *
    * @param containerId of the container that will be checked
    * @param timeout duration to be used until consider container unhealthy
    * @return true if container is healthy, otherwise false.
    */
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
        } else {
          Thread.sleep(1.seconds.toMillis)
        }
      }
      result.nonEmpty
    }

    try {
      Await.result(future, timeout)
    } catch {
      case e: TimeoutException =>
        logger.error(s"Container $containerId didn't change to healthy state in ${timeout.toSeconds} seconds", e)
        false
    }
  }

  /**
    * Waits for all project containers with health checks to be in an healthy state within the timeout interval.
    *
    * @param timeout duration to be used until consider container unhealthy
    * @return true if all containers are healthy, otherwise false.
    */
  def waitForAllHealthyContainers(timeout: Duration): Boolean = {
    getProjectContainerIds()
      .filter(isContainerWithHealthCheck)
      .forall(waitForHealthyContainer(_, timeout))
  }

  /**
    * Returns all the logs or just service logs
    *
    * @param serviceName to retrieve logs
    * @return a list of strings containing the logs if run with success, empty list otherwise.
    */
  def getContainerLogs(serviceName: Option[String]): List[String] = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "logs", "--no-color") ++
        serviceName

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output
      case Failure(f) =>
        serviceName.foreach(name => logger.error(s"Failed while retrieving logs of $name", f))
        List.empty
    }
  }

  /**
    * Returns the IP Address of the container.
    *
    * @param id of the container
    * @return an InetAddress option if ip was discovered. Otherwise None.
    */
  def getContainerAddress(id: String): Option[InetAddress] = {
    getAllNetworkIds().collectFirst {
      case networkId if { getContainerAddress(id, networkId).isDefined } => getContainerAddress(id, networkId).get
    }
  }

  /**
    * Returns the IP Address (InetAddress) of the container.
    *
    * @param id of the container
    * @param networkId containing the container
    * @return an InetAddress option if ip was discovered. Otherwise None.
    */
  private def getContainerAddress(id: String, networkId: String): Option[InetAddress] = {
    val cmd = Seq("docker", "inspect", "-f", s"""{{(index .Containers "$id").IPv4Address}}""", networkId)
    runCmdWithOutput(cmd, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(Seq(ip, _*)) if ip.nonEmpty => Try(InetAddress.getByName(ip.replaceAll("\\/.*", ""))).toOption
      case _ => None
    }
  }

  /**
    * Checks that all project containers have been removed.
    *
    * @return true if no containers remaining, false otherwise.
    */
  def checkContainersRemoval(): Boolean = {
    val command =
      Seq("docker-compose") ++
        composeFileArguments(composeFiles) ++
        Seq("-p", projectName, "ps", "-q")

    runCmdWithOutput(command, workingDirectory.toFile, environment, DefaultShortCommandTimeOut) match {
      case Success(output) =>
        output.isEmpty
      case Failure(f) =>
        logger.error("Failed while checking containers removal", f)
        false
    }
  }

  /**
    * Stops all containers.
    *
    * @param ids of the containers to be stopped
    * @return true if all the containers stopped with success, false otherwise.
    */
  private def stopAllContainers(ids: Seq[String]): Boolean = {
    ids.forall { id =>
      val command = Seq("docker", "stop", id)
      runCmd(command, workingDirectory.toFile, Map.empty, DefaultLongCommandTimeOut)
    }
  }

  /**
    * Removes all containers.
    *
    * @param ids of the containers to be stopped
    * @return true if all the containers stopped with success, false otherwise.
    */
  private def removeAllContainers(ids: Seq[String]): Boolean = {
    ids.forall { id =>
      val command = Seq("docker", "rm", "-f", id)
      runCmd(command, workingDirectory.toFile, Map.empty, DefaultLongCommandTimeOut)
    }
  }

  /**
    * Fetches all network Ids.
    *
    * @return a list of network ids.
    */
  private def getAllNetworkIds(): List[String] = {
    val command = Seq("docker", "network", "ls", "-q")
    runCmdWithOutput(command, workingDirectory.toFile, Map.empty, DefaultShortCommandTimeOut) match {
      case Success(list) => list
      case Failure(_)    => List.empty
    }
  }

  /**
    * Checks if network belongs to the project.
    *
    * @return true if it belongs, otherwise false.
    */
  private def isNetworkFromProject(id: String): Boolean = {
    val command = Seq("docker", "network", "inspect", "-f", "{{ index .Labels \"com.docker.compose.project\"}}", id)
    runCmdWithOutput(command, workingDirectory.toFile, Map.empty, DefaultShortCommandTimeOut) match {
      case Success(Seq(`projectName`, _*)) => true
      case _ => false
    }
  }

  /**
    * Removes the network.
    *
    * @param networkId to be removed
    * @return true if network was successfully removed, otherwise false.
    */
  private def removeNetwork(networkId: String): Boolean = {
    val command = Seq("docker", "network", "rm", networkId)
    runCmd(command, workingDirectory.toFile, Map.empty, DefaultShortCommandTimeOut)
  }

  /**
    * Executes a process and waits until it be completed within a certain time interval.
    *
    * @param process to be executed
    * @param timeout interval to consider process has failed
    * @return process exit value.
    */
  private def waitProcessExit(process: Process, timeout: Duration): Int = {
    val future = Future(blocking(process.exitValue()))
    try {
      Await.result(future, timeout)
    } catch {
      case e: TimeoutException =>
        logger.error(s"Process didn't finish in ${timeout.toSeconds} seconds", e)
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
  private def runCmdWithOutput(command: Seq[String],
                       cwd: File,
                       envVars: Map[String, String],
                       timeout: Duration): Try[List[String]] = {
    Try {
      val resultBuffer = new ListBuffer[String]()
      val process =
        Process(command, cwd, envVars.toSeq: _*)
          .run(ProcessLogger(line => resultBuffer += line, line => logger.debug(line)))

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
  private def runCmd(command: Seq[String], cwd: File, envVars: Map[String, String], timeout: Duration): Boolean = {
    val process =
      Process(command, cwd, envVars.toSeq: _*).run(ProcessLogger(_ => (), line => logger.debug(line)))

    waitProcessExit(process, timeout) == 0
  }

  /**
    * Maps a list of Paths into a list of argument strings to be used by docker-compose command
    *
    * @param files to be used as docker-compose arguments
    * @return a list of docker-compose arguments
    */
  private def composeFileArguments(files: Seq[Path]): Seq[String] = {
    files.flatMap(file => Seq("-f", file.toString))
  }
}
