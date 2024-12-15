package com.sheryv.tools.webcrawler.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchItem(
  val id: Long = 0,
  val name: String,
  @JsonProperty("original_name")
  val originalName: String,
  val overview: String,
  @JsonProperty("original_language")
  val originalLanguage: String,
  val popularity: Double = 0.0,
  @JsonProperty("vote_average")
  val voteAverage: Double = 0.0,
  @JsonProperty("vote_count")
  val voteCount: Long = 0,
  @JsonProperty("first_air_date")
  val firstAirDate: String? = null,
  @JsonProperty("poster_path")
  val posterPath: String? = null,
  val season: List<TmdbSeason> = emptyList(),
  @JsonProperty("external_ids")
  val externalIds: Map<String, Any> = emptyMap()
) {
  
  fun posterUrl() = "http://image.tmdb.org/t/p/w500$posterPath"
  
  fun imdbId() = externalIds["imdb_id"]?.toString()
  
  override fun toString(): String {
    return String.format("%-40s | %2.1f [%s] %7d (%.1f) %d", name, popularity, firstAirDate, id, voteAverage, voteCount)
  }
}
