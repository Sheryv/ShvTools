package com.sheryv.tools.subtitlestranslator.subsdownload;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Options {
    private String urlFormatString;
    private String baseUrl;
    private String downloadDirectory;
    private String temporaryDirectory;

    private String seriesId;
    private String series;
    private int season;
    private int episode;

    public String formatFullUrl() {
        return baseUrl + String.format(urlFormatString, seriesId, season, episode);
    }

    public static Options getDefault() {
        Options options = new Options();
        options.setBaseUrl("https://www.opensubtitles.org")
                .setTemporaryDirectory("C:\\temp")
                .setUrlFormatString("/pl/ssearch/sublanguageid-pol,eng/searchonlytvseries-on/season-%2$d/episode-%3$d/idmovie-%1$s");
        return options;
    }
}
