package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.utils.Utils
import com.sheryv.util.FileUtils
import com.sheryv.util.Strings
import java.time.OffsetDateTime

data class Episode(
  val title: String,
  val number: Int,
  val downloadUrl: VideoUrl? = null,
  val sourcePageUrl: String,
  val type: EpisodeAudioTypes? = null,
  val format: EpisodeFormat? = null,
  val errors: List<ErrorEntry> = emptyList(),
  val created: OffsetDateTime = Utils.now()
) {
  @JsonIgnore
  var lastSize: Long = 0
  val updated: OffsetDateTime = Utils.now()
  
  fun generateFileName(series: Series,  settings: StreamingWebsiteSettings, fileExtension: String = downloadUrl?.defaultFileFormat()?.extension ?: "mp4"): String {
    var ext = fileExtension
    if (downloadUrl != null && Configuration.property("crawler.streaming.episode.get-extension-from-url").toBoolean()) {
      val indexOf = downloadUrl.base.lastIndexOf(".")
      if (indexOf > 0 && downloadUrl.base.length - indexOf <= 5) {
        ext = downloadUrl.base.substring(indexOf + 1, downloadUrl.base.length)
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
