package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.sheryv.util.FileUtils
import java.nio.file.Path
import java.time.LocalDate

data class Series(
  val title: String,
  val season: Int,
  val lang: String,
  val seriesUrl: String,
  val posterUrl: String? = null,
  val tvdbId: String? = null,
  val imdbId: String? = null,
  val releaseDate: LocalDate? = null,
  val episodes: List<Episode> = emptyList(),
) {
  
  fun generateDirectoryPathForSeason(): Path {
    return Path.of(FileUtils.fixFileNameWithCollonSupport(title), String.format("Season %02d", season))
  }
}
