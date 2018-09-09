package com.sheryv.tools.movielinkgripper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Series {
    private final String name;
    private final int season;
    private final List<Episode> episodes;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Series(@JsonProperty("name") String name, @JsonProperty("season") int season, @JsonProperty("episodes") List<Episode> episodes) {
        this.name = name;
        this.season = season;
        this.episodes = episodes;
    }
}
