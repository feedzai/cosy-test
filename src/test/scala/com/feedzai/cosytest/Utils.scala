package com.feedzai.cosytest

import scala.util.Random

object Utils {
  def randomSetupName: String = Random.alphanumeric.take(10).mkString.toLowerCase
}
