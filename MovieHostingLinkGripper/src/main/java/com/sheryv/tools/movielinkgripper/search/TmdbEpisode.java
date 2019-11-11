
package com.sheryv.tools.movielinkgripper.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbEpisode {

    @JsonProperty("air_date")
    private String airDate;
    @JsonProperty("episode_number")
    private long episodeNumber;
    private long id;
    private String name;
    private String overview;
    @JsonProperty("vote_average")
    private long voteAverage;
    @JsonProperty("vote_count")
    private long voteCount;

}
