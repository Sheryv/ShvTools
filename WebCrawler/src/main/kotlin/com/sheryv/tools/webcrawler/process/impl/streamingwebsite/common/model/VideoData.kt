package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import java.time.Instant


data class VideoData(
  val episodePageUrl: String,
  val title: String,
  val number: Int,
) {
  
  var pageOpenTimestamp: Instant? = null
  var server: VideoServer? = null
}
