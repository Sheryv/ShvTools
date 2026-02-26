package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common

import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import org.openqa.selenium.By

interface VideoServerDefinition {
  fun id(): String
  fun label(): String
  fun searchTerm(): Regex
  fun domains(): List<String>
  fun innerIframeCssSelector(): By?
  val isStreaming: Boolean
  fun isUrlMatchingRequestWithM3U8Manifest(url: String): Boolean
  suspend fun activatePlayer(crawler: SeleniumCrawler<*>): Boolean
}
