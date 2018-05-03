package com.feedzai.cosytest.scalatest

import java.nio.file.Path
import java.util.concurrent.Semaphore

import com.feedzai.cosytest.core.DockerComposeSetup
import org.scalatest.{Args, BeforeAndAfterAll, Status, TestSuite}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait DockerComposeTestSuite extends TestSuite with BeforeAndAfterAll {
  private val logger = LoggerFactory.getLogger(getClass)

  /**
   *  Optional Docker Compose Setup. If None docker will not be used
   */
  def dockerSetup: Option[DockerComposeSetup]

  def keepContainersOnSuccess: Boolean = false
  def keepContainersOnFailure: Boolean = false

  def logDumpLocation: Option[Path] = None
  def logDumpFileName: Option[String] = None

  def containerStartUpTimeout: Option[Duration] = None

  /**
   * Semaphore used to control the number of tests running in parallel
   */
  def parallelTestLimitSemaphore: Semaphore = new Semaphore(1, true)

  protected var testFailed: Boolean = false

  protected abstract override def runTest(testName: String, args: Args): Status = {
    val result = super.runTest(testName, args)
    if (!result.succeeds()) {
      testFailed = true
    }
    result
  }

  protected abstract override def beforeAll(): Unit = {
    super.beforeAll()
    parallelTestLimitSemaphore.acquire()
    dockerSetup.foreach { setup =>
      logger.info("Starting containers...")
      val started = setup.up(containerStartUpTimeout.getOrElse(5.minutes))
      testFailed = !started
      assert(started, s"Failed to start containers in test ${setup.projectName}!")
      logger.info("Containers started!")
    }
  }

  protected abstract override def afterAll(): Unit = {
    super.afterAll()
    try {

      if (testFailed) {
        for {
          setup <- dockerSetup
          dumpLocation <- logDumpLocation
          dumpFileName <- logDumpFileName
        } setup.dumpLogs(dumpFileName, dumpLocation) match {
          case Success(_) => ()
          case Failure(f) => logger.error("Failed to dump logs!", f)
        }
      }

      for (setup <- dockerSetup) {
        val keep = (keepContainersOnSuccess && !testFailed) || (keepContainersOnFailure && testFailed)

        if (!keep) {
          logger.info("Removing containers...")
          val removed = setup.down()
          assert(removed, s"Failed to remove containers in test ${setup.projectName}!")
          logger.info("Containers removed!")
        }
      }
    } finally { parallelTestLimitSemaphore.release() }
  }
}
