package com.sheryv.tools.subtitlestranslator;

import com.sheryv.tools.subtitlestranslator.subsdownload.Runner;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        try {
            File config;
            if (args.length >= 1) {
                if (args.length == 2)
                    config = new File(args[0]);
                else
                    config = new File(Configuration.CONFIG_FILE);
                Configuration configuration = SerialisationUtils.fromJson(FileUtils.readFileInMemory(config.toPath()), Configuration.class);
                Configuration.init(configuration);
                Runner runner = new Runner();
                Executors.newSingleThreadExecutor().submit(() -> {
                    runner.start(configuration.getOptions());
                });
            } else {
                config = new File(Configuration.CONFIG_FILE);
                FileUtils.saveFile(SerialisationUtils.toJsonPretty(Configuration.DEFAULT), config.toPath());
                System.out.println("Example file was written to " + config.getAbsolutePath());
                System.out.println("Use with param 's' to start");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
