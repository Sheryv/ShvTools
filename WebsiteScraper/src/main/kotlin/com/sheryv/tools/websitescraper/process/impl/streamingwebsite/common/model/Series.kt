package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model

data class Series(
  val title: String,
  val season: Int,
  val lang: String,
  val seriesUrl: String,
  val episodes: List<Episode> = emptyList()
)
