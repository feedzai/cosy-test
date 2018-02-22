package com.feedzai.cosytest


import java.nio.file.Path
import java.time.Duration
import java.util
import java.util.Optional
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

case class DockerComposeJavaSetup(
  setupName: String,
  composeFiles: util.List[Path],
  workingDirectory: Path ,
  environment: util.Map[String, String]
){

  val dockerSetup = DockerComposeSetup(setupName, composeFiles.asScala, workingDirectory, environment.asScala.toMap)

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
    * @param name       service name
    * @param bindedPort port that was binded to some specific port
    * @return Returns a collection of ports from all service containers that have binded port
    */
  def getServiceMappedPort(name: String, bindedPort: Int) =
    new util.ArrayList[String](dockerSetup.getServiceMappedPort(name, bindedPort).asJava)

  /**
    * Dump logs from STDOUT using docker-compose logs for each service. Logs will be saved to
    * target dir in a zip file with the name specified.
    *
    * @param fileName zip file name
    * @param target   directory where zip file will be saved
    * @return Try object that if fails contains exception, otherwise returns nothing
    */
  def dumpLogs(fileName: String, target: Path): Try[Unit] = dockerSetup.dumpLogs(fileName, target)

  /**
    * Removes network and containers started by setup if still present.
    *
    * @return true if setup network and containers were removed with success otherwise false
    */
  def cleanUp: Boolean = dockerSetup.cleanUp()

  def getContainerMappedPort(containerId: String, port: Int): String =
    dockerSetup.getContainerMappedPort(containerId, port)

  def getServiceContainerIds(serviceName: String) =
    new util.ArrayList[String](dockerSetup.getServiceContainerIds(serviceName).asJava)


  def getProjectContainerIds = new util.ArrayList[String](dockerSetup.getProjectContainerIds().asJava)

  def getServices = new util.ArrayList[String](dockerSetup.getServices().asJava)

  def isContainerWithHealthCheck(containerId: String): Boolean = dockerSetup.isContainerWithHealthCheck(containerId)

  def dockerComposeUp: Boolean = dockerSetup.dockerComposeUp()

  def dockerComposeDown: Boolean = dockerSetup.dockerComposeDown()

  def waitForHealthyContainer(containerId: String, timeout: Duration): Boolean =
    dockerSetup.waitForHealthyContainer(containerId, new FiniteDuration(timeout.toNanos, TimeUnit.NANOSECONDS))

  def waitForAllHealthyContainers(timeout: Duration): Boolean =
    dockerSetup.waitForAllHealthyContainers(new FiniteDuration(timeout.toNanos, TimeUnit.NANOSECONDS))

  def getContainerLogs(serviceName: Optional[String]): util.ArrayList[String] = {
    if (serviceName.isPresent) {
      new util.ArrayList[String](dockerSetup.getContainerLogs(Option(serviceName.get())).asJava)
    } else {
      new util.ArrayList[String](dockerSetup.getContainerLogs(None).asJava)
    }
  }

  def checkContainersRemoval: Boolean = dockerSetup.checkContainersRemoval()

}
