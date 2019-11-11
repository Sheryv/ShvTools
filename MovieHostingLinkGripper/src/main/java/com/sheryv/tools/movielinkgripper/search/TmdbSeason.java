
package com.sheryv.tools.movielinkgripper.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbSeason {

    private String _id;
    @JsonProperty("air_date")
    private String airDate;
    private List<TmdbEpisode> episodes;

}
