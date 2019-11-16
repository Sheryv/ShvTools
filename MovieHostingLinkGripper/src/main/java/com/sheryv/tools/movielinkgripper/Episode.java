package com.sheryv.tools.movielinkgripper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.util.Strings;
import lombok.Getter;
import lombok.Setter;
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
    @Setter
    private String dlLink;
    private final Format format;
    private final String page;
    @Setter
    private long lastSize;

    public Episode(String page, String name, int n, String dlLink) {
        this(page, name, n, dlLink, 0, EpisodesTypes.UNKNOWN, null, null);
//        this(n, name, EpisodesTypes.UNKNOWN, 0, dlLink, page, null);
    }

    public Episode(String page, String name, int n, String dlLink, EpisodesTypes type) {
        this(page, name, n, dlLink, 0, type, null, null);
    }

    public Episode(String page, String name, int n, String dlLink, int error, EpisodesTypes type) {
        this(page, name, n, dlLink, error, type, null, null);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Episode(
            @JsonProperty("page") String page,
            @JsonProperty("name") String name,
            @JsonProperty("n") int n,
            @JsonProperty("dlLink") String dlLink,
            @JsonProperty("error") int error,
            @JsonProperty("type") EpisodesTypes type,
            @JsonProperty("format") @Nullable Format format,
            @JsonProperty("lastSize") @Nullable Long lastSize) {
        this.n = n;
        this.name = name;
        this.error = error;
        this.page = page;
        this.dlLink = dlLink;
        this.type = type;
        this.format = format;
        this.lastSize = lastSize == null ? 0 : lastSize;
    }

    public String generateFileName(Series series) {
        String ext = ".mp4";
        if (dlLink != null) {
            int indexOf = dlLink.lastIndexOf(".");
            if (dlLink.length() - indexOf <= 5) {
                ext = dlLink.substring(indexOf, dlLink.length());
            }
        }
        String name = getName();
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
        return Strings.fillTemplate(config.getEpisodeCodeFormatter() + nameFormatter, values)
                .replaceAll("[\\\\/:*?\"<>|]", "");
    }
}
