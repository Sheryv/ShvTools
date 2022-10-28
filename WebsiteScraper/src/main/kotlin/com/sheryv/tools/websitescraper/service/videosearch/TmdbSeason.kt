package com.sheryv.tools.websitescraper.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class TmdbSeason(
  val id: Long,
  val _id: String,
  val episodes: List<TmdbEpisode>,
  @JsonProperty("air_date")
  val airDate: String? = null,
) {
}
