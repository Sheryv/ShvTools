package com.sheryv.tools.webcrawler.service.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchResult<T>(
  val page: Long = 0,
  val results: List<T>,
  @JsonProperty("total_pages")
  val totalPages: Long,
  @JsonProperty("total_results")
  val totalResults: Long,
) {
}
