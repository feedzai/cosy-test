package com.feedzai.cosytest

import com.feedzai.cosytest.core.DockerComposeSetup
import org.scalatest.{BeforeAndAfterAll, TestSuite}
import org.slf4j.LoggerFactory

/**
 *   Trait used to clean up docker environment if tests leave system dirty.
 */
trait CleanUp extends TestSuite with BeforeAndAfterAll {

  private val logger = LoggerFactory.getLogger(getClass)

  def dockerSetups: Seq[DockerComposeSetup]

  protected abstract override def afterAll(): Unit = {
    super.afterAll()

    if (dockerSetups.forall(_.cleanUp())) {
      logger.info("System is clean!")
    } else {
      logger.error("System is dirty...")
    }
  }
}
