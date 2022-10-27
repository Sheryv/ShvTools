package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model

private const val UNKNOWN_NAME = "UNKNOWN"

data class VideoServer(
  val serverName: String,
  val index: Int = 0,
  val type: EpisodeTypes = EpisodeTypes.UNKNOWN,
  val format: EpisodeFormat? = null,
  val videoPageExternalUrl: String? = null,
) {
}
