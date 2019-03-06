package com.sheryv.tools.subtitlestranslator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sheryv.tools.subtitlestranslator.subsdownload.Runner;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        try {
            File config;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (args.length >= 1) {
                if (args.length == 2)
                    config = new File(args[0]);
                else
                    config = new File(Configuration.CONFIG_FILE);
                Configuration configuration = gson.fromJson(FileUtils.readFileToString(config), Configuration.class);
                Configuration.init(configuration);
                Runner runner = new Runner();
                Executors.newSingleThreadExecutor().submit(() -> {
                    runner.start(configuration.getOptions());
                });
            } else {
                config = new File(Configuration.CONFIG_FILE);
                FileUtils.writeStringToFile(config, gson.toJson(Configuration.DEFAULT));
                System.out.println("Example file was written to " + config.getAbsolutePath());
                System.out.println("Use with param 's' to start");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
