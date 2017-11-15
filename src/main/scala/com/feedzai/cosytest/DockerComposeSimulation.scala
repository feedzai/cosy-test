package com.feedzai.cosytest

import java.nio.file.Path

import io.gatling.core.Predef.Simulation

import scala.concurrent.duration._

trait DockerComposeSimulation extends Simulation {

  /**
   *  Optional Docker Compose Setup. If None docker will not be used
   */
  def dockerSetup: Option[DockerComposeSetup]

  def keepContainers: Boolean = false

  def logDumpLocation: Option[Path] = None
  def logDumpFileName: Option[String] = None

  def containerStartUpTimeout: Option[Duration] = None

  def beforeSimulation(): Unit = {
    dockerSetup.foreach(_.up(containerStartUpTimeout.getOrElse(5.minutes)))
  }

  after {
    for {
      setup <- dockerSetup
      dumpLocation <- logDumpLocation
      dumpFileName <- logDumpFileName
    }  setup.dumpLogs(dumpFileName, dumpLocation)

    if (!keepContainers){
      dockerSetup.foreach(_.down())
    }
  }
}
