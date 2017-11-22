package com.feedzai.cosytest

import java.nio.file.Paths

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 *   Trait used to clean up docker environment if tests leave system dirty.
 *
 *   Note: Shouldn't be used if tests run in parallel
 */
trait CleanUp extends TestSuite with BeforeAndAfterAll {

  private val logger = LoggerFactory.getLogger(getClass)

  private val WorkingDir = Paths.get("").toAbsolutePath
  private val setup = DockerComposeSetup("Dummy", Seq.empty, WorkingDir, Map.empty)

  protected abstract override def afterAll(): Unit = {
    super.afterAll()

    // Fetch containers ids if present
    val command = Seq("docker", "ps", "-aq")
    val fetchedIds = setup.runCmdWithOutput(command, WorkingDir.toFile, Map.empty, 10.seconds)

    val removedContainers = fetchedIds match {
      case Success(containers) => stopAllContainers(containers) && removeAllContainers(containers)
      case Failure(f) =>
        logger.warn("Failed to retrieve containers ids...", f)
        false
    }

    // Remove docker networks
    val removedNetworks = removeNetworks()

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

  private def removeNetworks(): Boolean = {
    val command = Seq("docker", "network", "prune", "-f")
    setup.runCmd(command, WorkingDir.toFile, Map.empty, 10.seconds)
  }
}
