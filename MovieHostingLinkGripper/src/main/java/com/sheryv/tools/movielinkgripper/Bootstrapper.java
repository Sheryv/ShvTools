package com.sheryv.tools.movielinkgripper;

import com.sheryv.tools.movielinkgripper.config.AbstractMode;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.ui.MainWindow;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;
import com.sheryv.util.logging.Lg;
import com.sheryv.util.logging.LoggingUtils;
import com.sheryv.util.property.PropertyUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class Bootstrapper {
    public static void main(String[] args) {
        try {
            if (args.length >= 3) {
                parse(args);
            } else if (args.length == 2) {
                String first = args[0];
                String sec = args[1];
                if ("c".equals(first)) {
                    runConfig(sec);
                } else if ("idm".equals(first)) {
                    File f = new File(sec);
                    if (!f.exists()) {
                        log.error("File does not exists");
                        return;
                    }
                    String str = FileUtils.readFileInMemory(Configuration.CONFIG_FILE);
                    Configuration configuration = Configuration.init(SerialisationUtils.fromYaml(str, Configuration.class));
                    Transformer.sendToIDM(Transformer.loadSeries(FileUtils.readFileInMemory(sec)), configuration);
                } else {
                    printDoc();
                }
            } else if (args.length == 1 && args[0].equals("c")) {
                runConfig(null);
            } else if (args[0].equals("i")) {
                log.info("Loading from file " + new File(Configuration.CONFIG_FILE).getAbsolutePath());
                if (!Files.exists(Paths.get(Configuration.CONFIG_FILE))) {
                    String s = SerialisationUtils.toYaml(Configuration.getDefault());
                    FileUtils.saveFile(s, Paths.get(Configuration.CONFIG_FILE));
                    log.info("Example config file was generated in " + new File(Configuration.CONFIG_FILE).getAbsolutePath());
                }
                String str = FileUtils.readFileInMemory(Configuration.CONFIG_FILE);
                Configuration.init(SerialisationUtils.fromYaml(str, Configuration.class));
                MainWindow.createAndShowGUI();
            } else {
                printDoc();
            }
        } catch (Exception e) {
            log.error("Error while parsing ", e);
        }
    }

    private static void runConfig(@Nullable String mode) throws Exception {
        if (mode == null || !Files.exists(Paths.get(Configuration.CONFIG_FILE))) {
            String s = SerialisationUtils.toYaml(Configuration.getDefault());
            FileUtils.saveFile(s, Paths.get(Configuration.CONFIG_FILE));
            log.info("Example config file was generated in " + new File(Configuration.CONFIG_FILE).getAbsolutePath());
        } else {
            log.info("Starting MovieLinkGripper from file " + new File(Configuration.CONFIG_FILE).getAbsolutePath());
            String str = FileUtils.readFileInMemory(Configuration.CONFIG_FILE);
            Configuration configuration = Configuration.init(SerialisationUtils.fromYaml(str, Configuration.class));
            for (AbstractMode abstractMode : configuration.getModes()) {
                if (mode.equals(abstractMode.getModeCommandLineName())) {
                    log.info("Executing " + abstractMode);
                    abstractMode.execute(configuration);
                    return;
                }
            }
            log.error("Indicated mode: " + mode + " was not found in configuration file at " + new File(Configuration.CONFIG_FILE).getAbsolutePath());
        }
    }

    private static void printDoc() {
        InputStream resourceAsStream = Bootstrapper.class.getClassLoader().getResourceAsStream("desc.txt");
        try {
            String s = new String(resourceAsStream.readAllBytes(), PropertyUtils.getCharset());
            File file = new File(Configuration.CONFIG_FILE);
            if (file.exists()) {
                String str = FileUtils.readFileInMemory(Configuration.CONFIG_FILE);
                Configuration configuration = SerialisationUtils.fromYaml(str, Configuration.class);
                String msg = "\n    &1&AVAILABLE MODES &0&from &3&" + file.getAbsolutePath() + "&0&:";
                for (AbstractMode abstractMode : configuration.getModes()) {
                    msg += "\n\t&2&" + abstractMode.getModeCommandLineName() + "&0& - " + abstractMode.getName();
                }
                s = String.format(s, msg);
            }
            Lg.colored(s);
        } catch (IOException e) {
            log.error("Error while printing docs", e);
        }
    }

    private static void parse(String[] args) throws IOException {
        String first = args[0];
        String filePath = args[1];
        String third = args[2];
        File f = new File(filePath);
        if (!f.exists()) {
            log.error("File with series does not exists");
            return;
        }
        if ("a".equals(first)) {

        } else if ("i".equals(first)) {
            if (args.length >= 4) {
                String fourth = args[3];
                int num = Integer.parseInt(third);
                log.info(Transformer.replaceLink(f.getAbsolutePath(), num, fourth) ? "Link replaced" : "Not replaced");
            } else {
                File source = new File(third);
                if (!source.exists()) {
                    log.error("File with new links does not exists");
                    return;
                }
                List<String> links = Files.readAllLines(source.toPath());
                for (String link : links) {
                    int index = link.indexOf(';');
                    int num = Integer.parseInt(link.substring(0, index));
                    String url = link.substring(index + 1, link.length());
                    log.info(Transformer.replaceLink(filePath, num, url) ? "Link replaced" : "Not replaced");
                }
            }

        } else if (first.startsWith("r") && args.length == 4) {
            log.error("Not supported");
//            String params = args[3];
//            String[] parts = params.split("\\|");
//            if (parts.length != 3) {
//                log.error("Incorrect series format [fourth argument]");
//                return;
//            }
//            int season = Integer.parseInt(parts[1]);
//            VideoProvider provider = Transformer.createProvider(third, parts[0], season, parts[2]);
//            Gripper.Options options = new Gripper.Options().setUseChrome(true).setStartEpisodeIndex(2);
//            if (first.contains("e")) {
//                options.setUseMoreProviders(true);
//            }
//            Gripper.create(options, provider);
        } else
            printDoc();

    }


    //            VideoProvider arrow = new FMoviesProvider("Arrow", 6, "/film/arrow-6.8n2jq");
//            VideoProvider alltube = new AlltubeProvider("Arrow", 6, "/serial/arrow-green-arrow-zielona-strzala/1163");
//            VideoProvider supergirl = createProvider("alltube", "Supergirl", 2, "/serial/supergirl/1856");

}
