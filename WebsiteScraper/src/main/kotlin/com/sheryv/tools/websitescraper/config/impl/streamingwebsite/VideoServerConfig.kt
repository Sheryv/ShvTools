package com.sheryv.tools.websitescraper.config.impl.streamingwebsite

data class VideoServerConfig(
  val name: String,
  val searchName: String = name,
  val enabled: Boolean = true
)
