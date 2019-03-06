package com.sheryv.tools.movielinkgripper.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sheryv.utils.Strings;
import lombok.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private static Configuration instance;

    public static Configuration init(Configuration c) {
        if (instance != null)
            throw new IllegalStateException("Already initialized");
        c.validate();
        instance = c;
        return instance;
    }

    public static Configuration get() {
        return instance;
    }


    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.PRIVATE)
    @JsonProperty(value = "_comment", access = JsonProperty.Access.READ_ONLY, index = -500)
    private String comment = "This is main configuration file for Movie Link Gripper tool";

    private int searchStartIndex = 1;

    private int searchStopIndex = -1;

    private boolean useChromeBrowser = true;

    private String defaultFilePathWithEpisodesList;

    private String episodeCodeFormatter;
    private String episodeNameFormatter;

    private String chromeExePath;
    private String chromeSeleniumDriverPath;
    private List<String> chromeExtensionsPaths;

    @JsonProperty(value = "startModes", index = 100)
    private Set<AbstractMode> modes;

    public Configuration(Set<AbstractMode> modes) {
        this.modes = modes;
    }

    private void validate() {
        if (this.getSearchStartIndex() <= 0) {
            throw new IllegalArgumentException("Search create index have to be greater than 0 and less than or equal to episodes count!");
        }
        if (this.getSearchStopIndex() < this.getSearchStartIndex() && this.getSearchStopIndex() != -1) {
            throw new IllegalArgumentException("Search stop index have to be greater than or equal to create index or equal to -1 for unlimited value");
        }
        if (Strings.isNullOrEmpty(this.getEpisodeCodeFormatter())) {
            throw new IllegalArgumentException("EpisodeCodeFormatter cannot be empty");
        }
        if (Strings.isNullOrEmpty(this.getChromeExePath()) || !new File(this.getChromeExePath()).exists()) {
            throw new IllegalArgumentException("Insert correct chrome.exe path in chromeExePath field");
        }
        if (Strings.isNullOrEmpty(this.getChromeSeleniumDriverPath()) || !new File(this.getChromeSeleniumDriverPath()).exists()) {
            throw new IllegalArgumentException("Insert correct Selenium chromedriver.exe path in chromeSeleniumDriverPath field");
        }
    }



    public static final String CONFIG_FILE = "movie_gripper.yaml";
    public static final Configuration DEFAULT;

    static {
        AddMode add = new AddMode();
        ReplaceMode repl = new ReplaceMode("G:\\links.csv");
        RunMode run = new RunMode("alltube", "Supergirl", 3, "/serial/supergirl/1856");
        SendToManagerMode send = new SendToManagerMode();
        run.setDirectoryToCompareIfIsAbsent("G:\\Filmy\\Serial\\Supergirl 03");
        DEFAULT = new Configuration(Set.of(add, repl, run, send));
        DEFAULT.setEpisodeCodeFormatter("%s S%02dE%02d");
        DEFAULT.setEpisodeNameFormatter(" - %s%s");
        DEFAULT.setDefaultFilePathWithEpisodesList("G:\\arrow_list.json");
        DEFAULT.setChromeExePath("F:\\__Programs\\Google\\Chrome\\Application\\chrome.exe");
        DEFAULT.setChromeSeleniumDriverPath("F:\\Data\\Selenium_drivers\\chromedriver.exe");
        DEFAULT.setChromeExtensionsPaths(Collections.singletonList("F:\\Data\\Selenium_drivers\\ublock_chrome_68.0.3440.106.crx"));
    }


}
