package com.sheryv.tools.webcrawler.config.impl

interface ApplicableEntry {
  val enabled: Boolean
  
  fun changeActivation(isEnabled: Boolean): ApplicableEntry
}
