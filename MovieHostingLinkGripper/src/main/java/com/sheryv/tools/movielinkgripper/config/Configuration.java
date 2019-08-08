package com.sheryv.tools.movielinkgripper.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;
import com.sheryv.util.Strings;
import lombok.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {

    private static boolean paused;
    private static Consumer<Boolean> onPausedChange;

    public static boolean isPaused() {
        return paused;
    }

    public static void setPaused(boolean paused) {
        Configuration.paused = paused;
        if (onPausedChange != null)
            onPausedChange.accept(paused);
    }

    public static void setOnPausedChange(Consumer<Boolean> onPausedChange) {
        Configuration.onPausedChange = onPausedChange;
    }

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
    private String idmExePath;

    private String chromeExePath;
    private String chromeSeleniumDriverPath;
    private List<String> chromeExtensionsPaths;
    private int triesBeforeHostingChange = 3;
    private int numOfTopHostingsUsedSimultaneously = 3;
    private List<String> availableHostings;
    private List<EpisodesTypes> allowedEpisodeTypes = Arrays.asList(EpisodesTypes.values());
    private List<HostingConfig> hostings;

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

    public RunMode findRunMode() {
        return (RunMode) Configuration.get().getModes().stream().filter(m -> m instanceof RunMode).findFirst().get();
    }


    public static final String CONFIG_FILE = "movie_gripper.yaml";
    public static final String CONFIG_TEMPLATE_FILE = "../../movie_gripper_template.yaml";

    public static Configuration getDefault() {
        AddMode add = new AddMode();
        ReplaceMode repl = new ReplaceMode("D:\\links.csv");
        RunMode run = new RunMode("alltube", "Arrow", 1, "");
        SendToManagerMode send = new SendToManagerMode();
        Configuration result = new Configuration(Set.of(add, repl, run, send));
        FileUtils.readFileInMemorySilently(Paths.get(CONFIG_TEMPLATE_FILE)).ifPresent(s -> {
            try {
                Configuration c = SerialisationUtils.fromYaml(s, Configuration.class);
                result.setAllowedEpisodeTypes(c.getAllowedEpisodeTypes());
                result.setHostings(c.getHostings());
                result.setTriesBeforeHostingChange(c.getTriesBeforeHostingChange());
                result.setAvailableHostings(c.getAvailableHostings());
                result.setEpisodeCodeFormatter(c.getEpisodeCodeFormatter());
                result.setEpisodeNameFormatter(c.getEpisodeNameFormatter());
                result.setIdmExePath(c.getIdmExePath());
                result.setDefaultFilePathWithEpisodesList(c.getDefaultFilePathWithEpisodesList());
                result.setChromeExePath(c.getChromeExePath());
                result.setChromeSeleniumDriverPath(c.getChromeSeleniumDriverPath());
                result.setChromeExtensionsPaths(c.getChromeExtensionsPaths());
                result.setUseChromeBrowser(c.isUseChromeBrowser());

                if (result.getHostings() == null) {
                    List<HostingConfig> list = new ArrayList<>();
                    List<String> resultAvailableHostings = result.getAvailableHostings();
                    for (int i = 0; i < resultAvailableHostings.size(); i++) {
                        String hosting = resultAvailableHostings.get(i);
                        list.add(new HostingConfig(hosting, (resultAvailableHostings.size() - i) * 10, hosting, true));
                    }
                    result.setHostings(list);
                } else {
                    for (String availableHosting : result.getAvailableHostings()) {
                        if (result.getHostings().stream().noneMatch(h -> h.getCode().equals(availableHosting))) {
                            HostingConfig min = result.getHostings().stream().min(Comparator.comparingInt(HostingConfig::getPriority)).get();
                            result.getHostings().add(new HostingConfig(availableHosting, min.getPriority() - 1, availableHosting, false));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return result;
    }


}
