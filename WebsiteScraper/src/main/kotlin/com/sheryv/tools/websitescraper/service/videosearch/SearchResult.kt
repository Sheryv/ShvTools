package com.sheryv.tools.websitescraper.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchResult(
  val page: Long = 0,
  val results: List<SearchItem>,
  @JsonProperty("total_pages")
  val totalPages: Long,
  @JsonProperty("total_results")
  val totalResults: Long,
) {
}
