package com.sheryv.tools.movielinkgripper;

import com.google.common.collect.Streams;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.provider.Hosting;
import com.sheryv.tools.movielinkgripper.provider.Item;
import com.sheryv.tools.movielinkgripper.provider.VideoProvider;
import com.sheryv.utils.FileUtils;
import com.sheryv.utils.SerialisationUtils;
import com.sheryv.utils.Strings;
import javafx.util.Pair;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class Gripper implements AutoCloseable {

    //    private static final ChromeOptions CHROME_OPTIONS = new ChromeOptions();
    private static final FirefoxOptions FIREFOX_OPTIONS = new FirefoxOptions();

    private static final int STOP_DELAY = 6;
    private final VideoProvider provider;
    private final JavascriptExecutor executor;
    @Getter
    private final WebDriverWait webWait;
    @Getter
    private final WebDriver driver;
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private Configuration configuration;
    @Getter
    private final Options options;

    private Gripper(Configuration configuration, Options options, VideoProvider provider) {
        this.configuration = configuration;
        this.options = options;
        this.provider = provider;
        if (configuration.isUseChromeBrowser())
            driver = new ChromeDriver(getChromeOptions());
        else
            driver = new FirefoxDriver(FIREFOX_OPTIONS);
        webWait = new WebDriverWait(driver, 15);
        driver.manage().timeouts().setScriptTimeout(3, TimeUnit.SECONDS);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
        executor = (JavascriptExecutor) driver;
    }

    public static Gripper create(Options options, VideoProvider provider) {
        Configuration configuration = Configuration.get();
        System.setProperty("webdriver.chrome.driver", configuration.getChromeSeleniumDriverPath());
        Gripper g = new Gripper(configuration, options, provider);
        g.provider.setGripper(g);
        return g;
    }

    public void close() {
        if (driver != null) {
            driver.close();
            driver.quit();
        }
    }

    public void start() {
        try {
            run();
        } catch (Exception e) {
            log.error("Error while running gripper", e);
        } finally {
            close();
        }
    }

    private void run() throws Exception {

//        beginStopLoading();
        driver.navigate().to(provider.getSeriesLink());
//            Thread.sleep(500);
        log.info("Run()");
        List<Item> items = provider.findEpisodesItems(null);
        Series series = new Series(provider.getSeries(), provider.getSeason(), provider.getMainLang(), new ArrayList<>(25));
        String json = null;
        int start = configuration.getSearchStartIndex();
        if (options.getRequiredIndexes() != null) {
            start = 1;
        }
        for (int i = start; i <= items.size() && i <= configuration.getSearchStopIndex(); i++) {
            Item item = items.get(i - 1);
            if (options.getRequiredIndexes() != null) {
                if (!options.getRequiredIndexes().contains(item.getNum())) {
                    log.info("Skipped " + item.toString());
                    continue;
                }
            }
            String downloadLink = null;
            int err = 0;
            try {
                provider.goToEpisodePage(item);
                List<Hosting> hostings = provider.loadItemDataFromSummaryPageAndGetVideoLinks(item);
                for (Hosting hosting : hostings) {
                    provider.openVideoPage(item, hosting.getVideoLink());
                    item.updateHosting(hosting);
                    try {
                        downloadLink = provider.findLoadedVideoDownloadUrl(item);
                        if (!Strings.isNullOrEmpty(downloadLink)) {
                            break;
                        }
                    } catch (Exception e) {
                        log.error("Error while searching dl url at " + hosting.getName() + " | " + hosting.getVideoLink(), e);
                    }
                }
            } catch (Exception e) {
                log.error("Error while searching dl url", e);
            }
            if (Strings.isNullOrEmpty(downloadLink)) {
                err = 2;
            }
            Episode ep = new Episode(item.getLink(), item.getName(), item.getNum(), downloadLink, err, item.getType());
            series.getEpisodes().add(ep);
            if (options.isSendToIdm())
                addToIDM(series, ep);
            json = SerialisationUtils.toJsonPretty(series);
            FileUtils.saveFile(json, Paths.get(configuration.getDefaultFilePathWithEpisodesList()));
            log.info("\n" + ep.toString() + "\n");
        }
        log.info(json);
    }


 /*   private void runForErrOnly() throws Exception {
        beginStopLoading();
        driver.navigate().to(provider.getSeriesLink());
        Series series = Transformer.loadSeries(FileUtils.readFileInMemory(configuration.getDefaultFilePathWithEpisodesList()));
        List<Episode> episodes = series.getEpisodes();
        for (int i = 0; i < episodes.size(); i++) {
            Episode episode = episodes.get(i);
            if (episode.getError() != 0) {
                log.info("Loading for " + episode);
                Item item = new Item(episode.getPage(), episode.getName(), episode.getN());
                provider.goToEpisodePage(item);
                provider.openVideoPage(item, );

                String downloadLink = provider.findLoadedVideoDownloadUrl(item);
                int err = 0;
                if (downloadLink == null) {
                    err = 2;
                }
                Episode ep = new Episode(item.getLink(), item.getName(), item.getNum(), downloadLink, err, item.getType());
                series.getEpisodes().set(i, ep);
            }
        }
        String json = SerialisationUtils.toJsonPretty(series);
        FileUtils.saveFile(json, Paths.get(configuration.getDefaultFilePathWithEpisodesList()));
    }*/

    public void beginStopLoading() {
        service.schedule(() -> {
            executor.executeScript("return window.stop");
        }, STOP_DELAY, TimeUnit.SECONDS);
    }

    public static void addToIDM(Series series, Episode episode) {
        String ex = String.format("\"F:\\__Programs\\Internet Download Manager\\IDMan.exe\" /n /f \"%s\" /p \"G:\\Filmy\\Serial\\%s %02d\"" +
                        " /a /d %s",
                episode.generateFileName(series),
                series.getName(),
                series.getSeason(),
                episode.getDlLink());
        try {
            log.info("\n> {} {}", episode.getN(), ex);
            Runtime.getRuntime().exec(ex);
        } catch (Exception e) {
            log.error("Error while adding to IDM", e);
        }
    }

    public Object executeScript(String script) {
        return executor.executeScript(script);
    }


    public <T> List<T> executeScriptFetchList(String script) {
        Object o = executeScript(script);
        if (o instanceof List) {
            @SuppressWarnings("unchecked") List<T> list = (List<T>) o;
            return list;
        }
        return null;
    }

    public List<Item> calculateEpisodesItemsWithNumSeparator(
            List<String> episodesLinks,
            List<String> episodesNames,
            Function<String, Pair<Integer, String>> separator,
            VideoProvider provider) {
        if (episodesNames == null || episodesNames.size() == 0) {
            episodesNames = Collections.nCopies(episodesLinks.size(), "");
        }
        return Streams.zip(episodesLinks.stream(), episodesNames.stream(), (link, ep) -> {
            Pair<Integer, String> linkAndNum = separator.apply(link);
            String u = linkAndNum.getValue();
            if (!linkAndNum.getValue().startsWith("/"))
                u = "/" + u;
            return new Item(provider.getProviderUrl() + u, ep == null ? "" : ep, linkAndNum.getKey());
        }).collect(Collectors.toList());
    }

    private ChromeOptions getChromeOptions() {
        DesiredCapabilities dc = new DesiredCapabilities();
        dc.setJavascriptEnabled(false);
        ChromeOptions options = new ChromeOptions();
        Configuration c = Configuration.get();
        options.setBinary(c.getChromeExePath());
        options.setCapability("applicationCacheEnabled", true);
        for (String s : c.getChromeExtensionsPaths()) {
            options.addExtensions(new File(s));
        }
        options.merge(dc);
        return options;
    }

    static {
        DesiredCapabilities dc = new DesiredCapabilities();
        dc = new DesiredCapabilities();
        dc.setJavascriptEnabled(false);
        FIREFOX_OPTIONS.setCapability("applicationCacheEnabled", true);
        FIREFOX_OPTIONS.merge(dc);
    }

    @Data
    @lombok.experimental.Accessors(chain = true)
    public static class Options {
        /**
         * Starts from 1
         */
        private List<Integer> requiredIndexes = null;
        private boolean sendToIdm = false;
        private boolean useMoreProviders = false;
    }

    /*
            System.setProperty("webdriver.gecko.driver", "F:\\Data\\geckodriver.exe")

        val episodesSel = By.cssSelector(SEL_EPISODES);
        val iframeSel = By.cssSelector(SEL_IFRAME);
        val videoSel = By.cssSelector(SEL_PLAYER);

        val driver = FirefoxDriver()
        driver.get(URL)
        val element = driver.findElements(episodesSel)
        element.forEach {
            println(it.text)

        }

//        println("Player " + playerUrl)
        val wait = WebDriverWait(driver, 5)
        wait.until {true}

        // Should see: "cheese! - Google Search"
        println("Page title is: " + driver.title)

        //Close the browser
        driver.quit()
    *
    * */
}
