package com.sheryv.tools.webcrawler.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class TmdbSeason(
  val id: Long,
  val _id: String,
  val episodes: List<TmdbEpisode>,
  val name: String,
  @JsonProperty("season_number")
  val season: Int,
  @JsonProperty("air_date")
  val airDate: String? = null,
  @JsonProperty("poster_path")
  val posterPath: String? = null,
) {
  
  fun posterUrl() = "http://image.tmdb.org/t/p/w500$posterPath"
}
