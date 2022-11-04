package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.VideoServerDefinition

data class VideoServer(
  val serverName: String,
  val index: Int = 0,
  val type: EpisodeAudioTypes = EpisodeAudioTypes.UNKNOWN,
  val format: VideoServerFormat = VideoServerFormat(),
  val videoPageExternalUrl: String? = null,
  var matchedServerDef: VideoServerDefinition? = null
) {
  
  override fun toString(): String {
    return "$serverName($index, $type, [$format])"
  }
}
