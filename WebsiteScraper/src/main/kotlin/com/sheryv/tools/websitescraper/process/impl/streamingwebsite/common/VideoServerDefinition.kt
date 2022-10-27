package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common

import com.sheryv.tools.websitescraper.config.impl.streamingwebsite.VideoServerConfig
import org.openqa.selenium.By

interface VideoServerDefinition {
  fun id(): String
  fun toConfig(): VideoServerConfig
  fun label(): String
  fun searchTerm(): String
  fun innerIframeCssSelector(): By?
  fun scriptToActivatePlayer(): String?
}
