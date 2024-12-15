package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

data class VideoData(
  val episodePageUrl: String,
  val title: String,
  val number: Int
) {
  
  var server: VideoServer? = null
}
