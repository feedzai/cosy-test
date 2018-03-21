package com.feedzai.cosytest.wrapper

import java.net.InetAddress
import java.nio.file.Path
import java.time.Duration
import java.util
import java.util.Optional
import java.util.concurrent.TimeUnit

import com.feedzai.cosytest.core.DockerComposeSetup

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * The Docker Compose setup to be managed and to interact with.
 *
 * @param projectName      The Docker Compose project name.
 * @param composeFiles     The Docker Compose files used to build the setup environment.
 * @param workingDirectory The Docker Compose working directory
 * @param environment      The environment variables provided to the setup environment.
 */
case class DockerComposeJavaSetup(
  projectName: String,
  composeFiles: util.List[Path],
  workingDirectory: Path,
  environment: util.Map[String, String]
){

  val dockerSetup = DockerComposeSetup(projectName, composeFiles.asScala, workingDirectory, environment.asScala.toMap)

  /**
    * Executes docker-compose up command using setup defined and waits for
    * all containers with healthchecks to be in a healthy state.
    *
    * @param timeout maximum time for all containers be in a healthy state
    * @return true if all containers started and achieved a healthy state otherwise false
    */
  def up(timeout: Duration): Boolean = dockerSetup.up(new FiniteDuration(timeout.toNanos, TimeUnit.NANOSECONDS))


  /**
    * Executes docker-compose down command using setup defined.
    *
    * @return true if all containers were stopped and removed otherwise false
    */
  def down: Boolean = dockerSetup.down()

  /**
    * Retrieves port that was mapped to binded port for specified service.
    *
    * @param name service name
    * @param bindedPort port that was binded to some specific port
    * @return Returns a list of ports from all service containers that have binded port
    */
  def getServiceMappedPort(name: String, bindedPort: Int) =
    new util.ArrayList[String](dockerSetup.getServiceMappedPort(name, bindedPort).asJava)

  /**
    * Dump logs from STDOUT using docker-compose logs for each service. Logs will be saved to
    * target dir in a zip file with the name specified.
    *
    * @param fileName zip file name
    * @param target directory where zip file will be saved
    * @return Try object that if fails contains exception, otherwise returns nothing
    */
  def dumpLogs(fileName: String, target: Path): Try[Unit] = dockerSetup.dumpLogs(fileName, target)

  /**
    * Removes network and containers started by setup if still present.
    *
    * @return true if setup network and containers were removed with success otherwise false
    */
  def cleanup: Boolean = dockerSetup.cleanup()

  /**
    * Returns the port open in localhost mapped to container port.
    *
    * @param containerId
    * @param port exposed from container
    * @return string of open localhost port. Empty if a failure occurs
    */
  def getContainerMappedPort(containerId: String, port: Int): String =
    dockerSetup.getContainerMappedPort(containerId, port)

  /**
    * Returns the list of container Ids associated to service.
    *
    * @param serviceName string
    * @return list of container ids. If a failure occurs an empty list is returned
    */
  def getServiceContainerIds(serviceName: String) =
    new util.ArrayList[String](dockerSetup.getServiceContainerIds(serviceName).asJava)

  /**
    * Returns the list of container IP Addresses associated to service.
    *
    * @param serviceName string
    * @return list of container ips.
    */
  def getServiceContainerAddresses(serviceName: String) =
    new util.ArrayList[InetAddress](dockerSetup.getServiceContainerAddresses(serviceName).asJava)

  /**
    * Returns the list of container Ids associated to the project.
    *
    * @return list of container ids. If a failure occurs an empty list is returned.
    */
  def getProjectContainerIds = new util.ArrayList[String](dockerSetup.getProjectContainerIds().asJava)

  /**
    * Returns the list of container IP Addresses associated to the project.
    *
    * @return list of container ips.
    */
  def getProjectContainerAddresses() = new util.ArrayList[InetAddress](dockerSetup.getProjectContainerAddresses().asJava)

  /**
    * Returns the list of network Ids associated to the project.
    *
    * @return list of network ids. If a failure occurs an empty list is returned.
    */
  def getProjectNetworkIds() = new util.ArrayList[String](dockerSetup.getProjectNetworkIds().asJava)

  /**
    * Returns the list of services associated to the project.
    *
    * @return list of services. If a failure occurs an empty list is returned
    */
  def getServices = new util.ArrayList[String](dockerSetup.getServices().asJava)

  /**
    * Checks if container has an health check.
    *
    * @return true if an health check is found, otherwise false.
    */
  def isContainerWithHealthCheck(containerId: String): Boolean = dockerSetup.isContainerWithHealthCheck(containerId)

  /**
    * Runs `docker-compose up` command
    *
    * @return true if command has run with success, otherwise false.
    */
  def dockerComposeUp: Boolean = dockerSetup.dockerComposeUp()

  /**
    * Runs `docker-compose down` command
    *
    * @return true if command has run with success, otherwise false.
    */
  def dockerComposeDown: Boolean = dockerSetup.dockerComposeDown()

  /**
    * Waits for container to be in an healthy state within the timeout interval.
    *
    * @param containerId of the container that will be checked
    * @param timeout duration to be used until consider container unhealthy
    * @return true if container is healthy, otherwise false.
    */
  def waitForHealthyContainer(containerId: String, timeout: Duration): Boolean =
    dockerSetup.waitForHealthyContainer(containerId, new FiniteDuration(timeout.toNanos, TimeUnit.NANOSECONDS))

  /**
    * Waits for all project containers with health checks to be in an healthy state within the timeout interval.
    *
    * @param timeout duration to be used until consider container unhealthy
    * @return true if all containers are healthy, otherwise false.
    */
  def waitForAllHealthyContainers(timeout: Duration): Boolean =
    dockerSetup.waitForAllHealthyContainers(new FiniteDuration(timeout.toNanos, TimeUnit.NANOSECONDS))

  /**
    * Returns all the logs or just service logs
    *
    * @param serviceName to retrieve logs
    * @return a list of strings containing the logs if run with success, empty list otherwise.
    */
  def getContainerLogs(serviceName: Optional[String]): util.ArrayList[String] = {
    if (serviceName.isPresent) {
      new util.ArrayList[String](dockerSetup.getContainerLogs(Option(serviceName.get())).asJava)
    } else {
      new util.ArrayList[String](dockerSetup.getContainerLogs(None).asJava)
    }
  }

  /**
    * Returns the IP Address of the container.
    *
    * @param id of the container
    * @return an InetAddress optional if ip was discovered.
    */
  def getContainerAddress(id: String) = Optional.ofNullable(dockerSetup.getContainerAddress(id).orNull)

  /**
    * Checks that all project containers have been removed.
    *
    * @return true if no containers remaining, false otherwise.
    */
  def checkContainersRemoval: Boolean = dockerSetup.checkContainersRemoval()

}
