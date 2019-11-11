
package com.sheryv.tools.movielinkgripper.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResult {

    private long page;
    private List<SearchItem> results;
    @JsonProperty("total_pages")
    private long totalPages;
    @JsonProperty("total_results")
    private long totalResults;

}
