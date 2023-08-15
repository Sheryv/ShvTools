package com.sheryv.tools.cmd.convertmovienames.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sheryv.tools.cmd.convertmovienames.videosearch.TmdbEpisode

@JsonIgnoreProperties(ignoreUnknown = true)
class TmdbSeason(
  val id: Long,
  val _id: String,
  val episodes: List<TmdbEpisode>,
  @JsonProperty("air_date")
  val airDate: String? = null,
) {
}
