package com.feedzai.cosytest

import jodd.util.RandomString

object Utils {
  def randomSetupName: String = RandomString.getInstance().randomAlphaNumeric(10).toLowerCase
}
