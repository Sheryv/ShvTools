package com.sheryv.tools.movielinkgripper.config;


import com.sheryv.tools.movielinkgripper.Gripper;
import com.sheryv.tools.movielinkgripper.Transformer;
import com.sheryv.tools.movielinkgripper.provider.VideoProvider;
import com.sheryv.util.Strings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
@Slf4j
public class RunMode extends AbstractMode {

    private static final Pattern EPISODE_INDEX_FROM_NAME = Pattern.compile("[S|s]\\d\\d[E|e](\\d\\d)");
    public static final String NAME = "Run";

    private String providerName = "alltube";
    private String seriesName = "Arrow";
    private int seasonIndex = 1;
    private String relativeUrlToSeries = "/";
    @Setter
    private boolean findOnlyForAbsent = false;
    @Setter
    private String directoryToCompareIfIsAbsent = "/";

    private RunMode() {
        super(NAME, "s");
    }

    public RunMode(String providerName, String seriesName, int seasonIndex, String relativeUrlToSeries) {
        this();
        this.providerName = providerName;
        this.seriesName = seriesName;
        this.seasonIndex = seasonIndex;
        this.relativeUrlToSeries = relativeUrlToSeries;
    }


    @Override
    public void execute(Configuration configuration) throws Exception {
        VideoProvider provider = Transformer.createProvider(providerName, seriesName, seasonIndex, relativeUrlToSeries);
        Gripper.Options options = new Gripper.Options();
        if (!Strings.isNullOrEmpty(filePathWithEpisodesList)) {
            configuration.setDefaultFilePathWithEpisodesList(filePathWithEpisodesList);
        }

        if (findOnlyForAbsent) {
            Path p = Paths.get(directoryToCompareIfIsAbsent);
            if (Files.exists(p) && Files.isDirectory(p)) {
                List<Integer> chosen = new ArrayList<>(configuration.getSearchStopIndex());
                for (int i = configuration.getSearchStartIndex(); i <= configuration.getSearchStopIndex(); i++) {
                    chosen.add(i);
                }
                for (Path path : Files.list(p).collect(Collectors.toList())) {
                    Matcher matcher = EPISODE_INDEX_FROM_NAME.matcher(path.getFileName().toString());
                    if (matcher.find()) {
                        int e = Integer.parseInt(matcher.group(1));
                        chosen.remove((Integer) e);
                    }

                }
                options.setRequiredIndexes(chosen);
            } else {
                log.error("Incorrect directory path at directoryToCompareIfIsAbsent: " + directoryToCompareIfIsAbsent);
            }
        }
        Gripper.create(options, provider).start();
    }
}
