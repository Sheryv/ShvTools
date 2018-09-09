package com.sheryv.tools.movielinkgripper.config;

import com.sheryv.tools.movielinkgripper.Transformer;
import com.sheryv.utils.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Getter
@Slf4j
public class ReplaceMode extends AbstractMode {

    public static final String NAME = "Replace";

    private String csvFilePathWithLinksToReplace = "G:\\links.csv";

    private ReplaceMode() {
        super(NAME, "r");

    }

    public ReplaceMode(String csvFilePathWithLinksToReplace) {
        this();
        this.csvFilePathWithLinksToReplace = csvFilePathWithLinksToReplace;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void execute(Configuration configuration) throws Exception {
        if (Strings.isNullOrEmpty(csvFilePathWithLinksToReplace) || !Files.exists(Paths.get(csvFilePathWithLinksToReplace))) {
            log.error("Path is incorrect at csvFilePathWithLinksToReplace: " + csvFilePathWithLinksToReplace);
            throw new IllegalArgumentException("Wrong format of csvFilePathWithLinksToReplace");
        }

        List<String> links = Files.readAllLines(new File(csvFilePathWithLinksToReplace).toPath());
        for (String link : links) {
            int index = link.indexOf(';');
            int num = Integer.parseInt(link.substring(0, index));
            String url = link.substring(index + 1, link.length());
            log.info(Transformer.replaceLink(csvFilePathWithLinksToReplace, num, url) ? "Link replaced" : "Not replaced");
        }
    }
}
