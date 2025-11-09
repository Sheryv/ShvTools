package com.sheryv.tools.webcrawler.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieSearchItem(
  val id: Long = 0,
  @JsonProperty("title")
  val name: String,
  @JsonProperty("original_title")
  val originalName: String,
  val overview: String,
  @JsonProperty("original_language")
  val originalLanguage: String,
  val popularity: Double = 0.0,
  @JsonProperty("vote_average")
  val voteAverage: Double = 0.0,
  @JsonProperty("vote_count")
  val voteCount: Long = 0,
  @JsonProperty("release_date")
  val releaseDate: String? = null,
  @JsonProperty("poster_path")
  val posterPath: String? = null,
  @JsonProperty("external_ids")
  val externalIds: Map<String, Any> = emptyMap()
) {
}
