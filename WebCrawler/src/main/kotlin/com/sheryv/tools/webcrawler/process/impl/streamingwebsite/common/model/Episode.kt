package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.util.DateUtils
import com.sheryv.util.io.FileUtils
import com.sheryv.util.Strings
import com.sheryv.util.fx.lib.objectProperty
import com.sheryv.util.fx.lib.stringProperty
import java.time.OffsetDateTime

data class Episode(
  val id: Long,
  val title: String,
  val number: Int,
  val downloadUrl: VideoUrl? = null,
  val sourcePageUrl: String,
  val type: EpisodeAudioTypes? = null,
  val format: EpisodeFormat? = null,
  val errors: List<ErrorEntry> = emptyList(),
  val created: OffsetDateTime = DateUtils.now()
) {
  @JsonIgnore
  val lastSize = objectProperty(0L)
  
  @JsonIgnore
  val url = stringProperty(downloadUrl?.toString() ?: "")
  
  val updated: OffsetDateTime = DateUtils.now()
  
  fun generateFileName(
    series: Series,
    settings: StreamingWebsiteSettings,
    fileExtension: String = downloadUrl?.resolveFileExtension() ?: "mp4"
  ): String {
    val values: MutableMap<String, Any> = LinkedHashMap()
    values["series_name"] = series.title
    values["season"] = String.format("%02d", series.season)
    values["episode_number"] = String.format("%02d", number)
    values["episode_name"] = title
    values["file_extension"] = ".$fileExtension"
    return FileUtils.fixFileNameWithColonSupport(
      Strings.fillTemplate(
        settings.episodeCodeFormatter + settings.episodeNameFormatter,
        values
      )
    )
  }
}
