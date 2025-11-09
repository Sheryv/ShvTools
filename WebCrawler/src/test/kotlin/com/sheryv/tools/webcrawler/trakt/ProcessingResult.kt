package com.sheryv.tools.webcrawler.trakt

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.webcrawler.RunFilmwebScraper
import java.time.LocalDate

private val DEFAULT_DATE = LocalDate.of(2023, 1, 1)

data class ProcessingResult(
  val url: String,
  val watchlist: Boolean,
  val title: String,
  val polishTitle: String?,
  val type: String,
  val year: Int,
  val episodesWatched: RunFilmwebScraper.Ratio? = null,
  val rateDate: LocalDate? = null,
  val rate: RunFilmwebScraper.Ratio? = null,
  val tmdbId: Int? = null,
  val imdbId: String? = null,
  val seasonSizes: List<Int>? = null,
  var watchedSingleEpisodes: List<Episode>? = null
) {
  
  @JsonIgnore
  fun isFavorite(): Boolean = rate == null && rateDate != null
  
  @JsonIgnore
  fun watchDate(): LocalDate? = rateDate?.takeIf { type == "film" }
  
  @JsonIgnore
  fun watchlistedDate(): LocalDate? = if (watchlist) rateDate?.let { minOf(defaultWatchDate(), it) } ?: defaultWatchDate() else null
  
  @JsonIgnore
  fun getEpisodesToSearch(): List<Episode> {
    if (episodesWatched == null) return emptyList()
    
    var sum = 0
    var lastWatchedSeason = 0
    for ((i, size) in seasonSizes!!.withIndex()) {
      sum += size
      if (sum >= episodesWatched.current) {
        lastWatchedSeason = i + 1
        if (sum > episodesWatched.current) {
          sum -= size
        }
        break
      }
    }
    if (lastWatchedSeason == 0) {
      sum = episodesWatched.current
    }
    
    return (1..(episodesWatched.current - sum)).map { Episode(lastWatchedSeason, it) }
  }
  
  @JsonIgnore
  fun getWatchedSeasons(): IntRange? {
    if ((episodesWatched?.current ?: 0) > 0 && seasonSizes == null) {
      println("Incorrect object $title - has watched episodes but no seasons list")
      return null
    }
    if (seasonSizes == null) {
      return null
    }
    return getEpisodesToSearch().firstOrNull()?.let { 1..<it.season } ?: seasonSizes?.size?.let { 1..it }
  }
  
  @JsonIgnore
  fun defaultWatchDate() = DEFAULT_DATE
}

data class Episode(val season: Int, val episodeNum: Int, val id: Long? = null, val imdbId: String? = null) {
  init {
    require(season > 0) { "Season must be greater than 0" }
    require(episodeNum > 0) { "Episode must be greater than 0" }
  }
}
