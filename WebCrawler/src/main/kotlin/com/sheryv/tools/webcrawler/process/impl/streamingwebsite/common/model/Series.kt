package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.sheryv.util.io.FileUtils
import java.nio.file.Path
import java.time.LocalDate

data class Series(
  val id: Long,
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
    return Path.of(FileUtils.fixFileNameWithColonSupport(title), String.format("Season %02d", season))
  }
  
  fun formattedString(): String {
    var output = """Series title: $title
      |Season: $season, ID: $id
      |
      |Episodes: ${episodes.size}
      |
    """.trimMargin()
    
    output += episodes.joinToString("\n") {
      "%2d. %-40s | %7d | %s".format(it.number, it.title, it.id, it.downloadUrl.toString())
    }
    return output
  }
}
