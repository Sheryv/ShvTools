package com.sheryv.tools.cmd.convertmovienames.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

interface SearchItem {
  val id: Long
  val posterPath: String?
  val popularity: Double
  
  fun name(): String
  
  fun date(): LocalDate?
  
  fun posterUrl() = "http://image.tmdb.org/t/p/w500$posterPath"
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TvSearchItem(
  override val id: Long,
  val name: String,
  @JsonProperty("original_name")
  val originalName: String,
  val overview: String,
  @JsonProperty("original_language")
  val originalLanguage: String,
  override val popularity: Double = 0.0,
  @JsonProperty("vote_average")
  val voteAverage: Double = 0.0,
  @JsonProperty("vote_count")
  val voteCount: Long = 0,
  @JsonProperty("first_air_date")
  val firstAirDate: LocalDate? = null,
  @JsonProperty("poster_path")
  override val posterPath: String? = null,
) : SearchItem {
  
  override fun name(): String = name
  
  override fun date(): LocalDate? = firstAirDate
  
  override fun toString(): String {
    return String.format("%-40s | %4.1f [%s] (%.1f) %7d | %d", name, popularity, firstAirDate, voteAverage, voteCount, id)
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MovieSearchItem(
  override val id: Long,
  val title: String,
  @JsonProperty("original_title")
  val originalTitle: String,
  val overview: String,
  @JsonProperty("original_language")
  val originalLanguage: String,
  override val popularity: Double = 0.0,
  @JsonProperty("vote_average")
  val voteAverage: Double = 0.0,
  @JsonProperty("vote_count")
  val voteCount: Long = 0,
  @JsonProperty("release_date")
  val releaseDate: LocalDate? = null,
  @JsonProperty("poster_path")
  override val posterPath: String? = null,
) : SearchItem {
  
  override fun name(): String = title
  
  override fun date(): LocalDate? = releaseDate
  
  override fun toString(): String {
    return String.format("%-40s | %2.1f [%s] (%.1f) %7d | %d", title, popularity, releaseDate, voteAverage, voteCount, id)
  }
}
