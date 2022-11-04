package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import org.openqa.selenium.By

interface VideoServerDefinition {
  fun id(): String
  fun label(): String
  fun searchTerm(): String
  fun domain(): String
  fun innerIframeCssSelector(): By?
  fun scriptToActivatePlayer(): String?
}
