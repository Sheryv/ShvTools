package com.sheryv.tools.subtitlestranslator;

import com.sheryv.tools.subtitlestranslator.subsdownload.Options;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
public class Configuration {

    private static Configuration instance;

    public static Configuration init(Configuration c) {
        if (instance != null)
            throw new IllegalStateException("Already initialized");
        System.setProperty("webdriver.chrome.driver", c.chromeSeleniumDriverPath);
        instance = c;
        return instance;
    }

    public static Configuration get() {
        return instance;
    }

    private String chromeExePath;
    private String chromeSeleniumDriverPath;
    private List<String> chromeExtensionsPaths;
    private Options options;

    public static final String CONFIG_FILE = "config.json";
    public static final Configuration DEFAULT;

    static {
        DEFAULT = new Configuration();
        DEFAULT.setChromeExePath("F:\\__Programs\\Google\\Chrome\\Application\\chrome.exe");
        DEFAULT.setChromeSeleniumDriverPath("F:\\Data\\Selenium_drivers\\chromedriver.exe");
        DEFAULT.setChromeExtensionsPaths(Collections.singletonList("F:\\Data\\Selenium_drivers\\ublock_chrome_68.0.3440.106.crx"));
        DEFAULT.setOptions(Options.getDefault()
                .setDownloadDirectory("C:\\temp\\dest")
                .setSeries("Arrow")
                .setSeason(7)
                .setEpisode(4)
                .setSeriesId("137932"));
    }


}
