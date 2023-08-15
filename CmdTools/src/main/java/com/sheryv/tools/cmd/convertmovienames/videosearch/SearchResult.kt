package com.sheryv.tools.cmd.convertmovienames.videosearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchResult<R : SearchItem>(
  val page: Long = 0,
  val results: List<R>,
  @JsonProperty("total_pages")
  val totalPages: Long,
  @JsonProperty("total_results")
  val totalResults: Long,
) {
}
