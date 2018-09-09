package com.sheryv.tools.movielinkgripper.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Set;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    @Setter(value = AccessLevel.NONE)
    @Getter(value = AccessLevel.PRIVATE)
    @JsonProperty(value = "_comment", access = JsonProperty.Access.READ_ONLY, index = -500)
    private String comment = "This is main configuration file for Movie Link Gripper tool";

    private int searchStartIndex = 1;

    private int searchStopIndex = -1;

    private boolean useChromeBrowser = true;

    private String defaultFilePathWithEpisodesList = "G:\\arrow_list.json";

    @JsonProperty(value = "startModes", index = 100)
    private Set<AbstractMode> modes;

    public Configuration(Set<AbstractMode> modes) {
        this.modes = modes;
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
    }
}
