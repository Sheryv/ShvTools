package com.sheryv.tools.websitescraper.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class TmdbEpisode(
  val id: Long,
  val name: String,
  val overview: String,
  @JsonProperty("episode_number")
  val episodeNumber: Int,
  @JsonProperty("air_date")
  val airDate: String? = null,
  @JsonProperty("vote_average")
  val voteAverage: Long = 0,
  @JsonProperty("vote_count")
  val voteCount: Long = 0,
) {
}
