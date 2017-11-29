package com.feedzai.cosytest

import java.nio.file.Paths

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 *   Trait used to clean up docker environment if tests leave system dirty.
 */
trait CleanUp extends TestSuite with BeforeAndAfterAll {

  private val logger = LoggerFactory.getLogger(getClass)

  private val WorkingDir = Paths.get("").toAbsolutePath
  private val setup = DockerComposeSetup("Dummy", Seq.empty, WorkingDir, Map.empty)

  def dockerSetups: Seq[DockerComposeSetup]

  protected abstract override def afterAll(): Unit = {
    super.afterAll()

    // Fetch containers ids if present
    val containerIds = dockerSetups.flatMap(setup => setup.getProjectContainerIds())

    if (containerIds.isEmpty) {
      logger.info("No containers to remove.")
    }

    val removedContainers = stopAllContainers(containerIds) && removeAllContainers(containerIds)

    // Remove docker networks
    val removedNetworks = dockerSetups.forall(
      setup =>
        getNetworkId(setup.setupName)
          .forall(removeNetwork)
    )

    // Log cleanup state
    if (removedContainers && removedNetworks) {
      logger.info("System is clean!")
    } else {
      if (!removedContainers) {
        logger.warn("Failed to remove containers...")
      }

      if (!removedNetworks) {
        logger.warn("Failed to remove networks...")
      }

      logger.warn("System is dirty...")
    }
  }

  private def stopAllContainers(ids: Seq[String]): Boolean = {
    ids.forall { id =>
      val command = Seq("docker", "stop", id)
      setup.runCmd(command, WorkingDir.toFile, Map.empty, 1.minute)
    }
  }

  private def removeAllContainers(ids: Seq[String]): Boolean = {
    ids.forall { id =>
      val command = Seq("docker", "rm", "-f", id)
      setup.runCmd(command, WorkingDir.toFile, Map.empty, 1.minute)
    }
  }

  private def getNetworkId(network: String): Option[String] = {
    val command = Seq("docker", "network", "ls", "--filter", s"name=${network}_default", "-q")
    setup.runCmdWithOutput(command, WorkingDir.toFile, Map.empty, 10.seconds) match {
      case Success(list) => if (list.nonEmpty) list.headOption else None
      case Failure(_)    => None
    }
  }

  private def removeNetwork(networkId: String): Boolean = {
    val command = Seq("docker", "network", "rm", networkId)
    setup.runCmd(command, WorkingDir.toFile, Map.empty, 10.seconds)
  }
}
