package com.sheryv.tools.movielinkgripper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.util.Strings;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Episode {
    private final int n;
    private final String name;
    private final EpisodesTypes type;
    private final int error;
    private final String dlLink;
    private final Format format;
    private final String page;

    public Episode(String page, String name, int n, String dlLink) {
        this(n, name, EpisodesTypes.UNKNOWN, 0, dlLink, page, null);
    }

    public Episode(String page, String name, int n, String dlLink, EpisodesTypes type) {
        this(n, name, type, 0, dlLink, page, null);
    }

    public Episode(String page, String name, int n, String dlLink, int error, EpisodesTypes type) {
        this(n, name, type, error, dlLink, page, null);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Episode(
            @JsonProperty("n") int n,
            @JsonProperty("name") String name,
            @JsonProperty("type") EpisodesTypes type,
            @JsonProperty("error") int error,
            @JsonProperty("dlLink") String dlLink,
            @JsonProperty("page") String page,
            @JsonProperty("format") @Nullable Format format) {
        this.n = n;
        this.name = name;
        this.error = error;
        this.page = page;
        this.dlLink = dlLink;
        this.type = type;
        this.format = format;
    }

    public String generateFileName(Series series) {
        String ext = ".mp4";
        if (dlLink != null) {
            int indexOf = dlLink.lastIndexOf(".");
            if (dlLink.length() - indexOf <= 5) {
                ext = dlLink.substring(indexOf, dlLink.length());
            }
        }
        String name = getName().replaceAll("[\\\\/:*?\"<>|]", "");
        String nameFormatter = "%5$s";
        Configuration config = Configuration.get();
        if (!Strings.isNullOrEmpty(config.getEpisodeNameFormatter())) {
            nameFormatter = config.getEpisodeNameFormatter();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("series_name", series.getName());
        values.put("season", String.format("%02d", series.getSeason()));
        values.put("episode_number", String.format("%02d", n));
        values.put("episode_name", name);
        values.put("file_extension", ext);
        return Strings.fillTemplate(config.getEpisodeCodeFormatter()+nameFormatter, values);
    }
}
