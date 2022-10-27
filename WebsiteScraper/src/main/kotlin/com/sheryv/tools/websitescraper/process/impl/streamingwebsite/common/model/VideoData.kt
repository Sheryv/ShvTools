package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model

data class VideoData(
  val episodePageUrl: String,
  val title: String,
  val number: Int
) {
  
  lateinit var server: VideoServer
}
