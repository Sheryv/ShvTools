
package com.sheryv.tools.movielinkgripper.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchItem {

    @JsonProperty("first_air_date")
    private String firstAirDate;
    private long id;
    private String name;
    @JsonProperty("original_language")
    private String originalLanguage;
    @JsonProperty("original_name")
    private String originalName;
    private String overview;
    private double popularity;
    @JsonProperty("vote_average")
    private double voteAverage;
    @JsonProperty("vote_count")
    private long voteCount;
  
  @Override
  public String toString() {
    return String.format("%-40s | %2.1f [%s] %7d (%.1f) %d", name, popularity, firstAirDate, id, voteAverage, voteCount);
  }
}
