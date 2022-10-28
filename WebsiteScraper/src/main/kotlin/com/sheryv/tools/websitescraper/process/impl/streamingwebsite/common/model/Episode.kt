package com.sheryv.tools.websitescraper.process.impl.streamingwebsite.common.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.websitescraper.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.websitescraper.utils.Utils
import com.sheryv.util.FileUtils
import com.sheryv.util.Strings
import java.time.OffsetDateTime

data class Episode(
  val title: String,
  val number: Int,
  val downloadUrl: String,
  val sourcePageUrl: String,
  val type: EpisodeTypes? = null,
  val format: EpisodeFormat? = null,
  val errors: List<ErrorEntry> = emptyList(),
  val created: OffsetDateTime = Utils.now()
) {
  @JsonIgnore
  var lastSize: Long = 0
  val updated: OffsetDateTime = Utils.now()
  
  fun generateFileName(series: Series, fileExtension: String, settings: StreamingWebsiteSettings): String {
    var ext = fileExtension
    if (downloadUrl.isNotEmpty()) {
      val indexOf = downloadUrl.lastIndexOf(".")
      if (indexOf > 0 && downloadUrl.length - indexOf <= 5) {
        ext = downloadUrl.substring(indexOf + 1, downloadUrl.length)
      }
    }
    val values: MutableMap<String, Any> = LinkedHashMap()
    values["series_name"] = series.title
    values["season"] = java.lang.String.format("%02d", series.season)
    values["episode_number"] = String.format("%02d", number)
    values["episode_name"] = title
    values["file_extension"] = ".$ext"
    return FileUtils.fixFileNameWithCollonSupport(
      Strings.fillTemplate(
        settings.episodeCodeFormatter + settings.episodeNameFormatter,
        values
      )
    )
  }
}
