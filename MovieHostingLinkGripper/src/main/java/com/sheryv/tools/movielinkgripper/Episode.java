package com.sheryv.tools.movielinkgripper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Episode {
    private final int num;
    private final String name;
    private final int error;
    private final String link;
    private final String downloadUrl;

    public Episode(String link, String name, int num, String downloadUrl) {
        this(link, name, num, downloadUrl, 0);
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Episode(
            @JsonProperty("link") String link,
            @JsonProperty("name") String name,
            @JsonProperty("num") int num,
            @JsonProperty("downloadUrl") String downloadUrl,
            @JsonProperty("error") int error) {
        this.num = num;
        this.name = name;
        this.error = error;
        this.link = link;
        this.downloadUrl = downloadUrl;
    }

    public String generateFileName(Series series) {
        String ext = ".mp4";
        if (downloadUrl != null) {
            int indexOf = downloadUrl.lastIndexOf(".");
            if (downloadUrl.length() - indexOf <= 5) {
                ext = downloadUrl.substring(indexOf, downloadUrl.length());
            }
        }
        return String.format("%s S%02dE%02d - %s%s", series.getName(), series.getSeason(), num, name, ext);
    }
}
