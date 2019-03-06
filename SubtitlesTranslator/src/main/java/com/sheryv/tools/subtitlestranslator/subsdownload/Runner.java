package com.sheryv.tools.subtitlestranslator.subsdownload;

import com.sheryv.tools.subtitlestranslator.Configuration;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

public class Runner {
    private JavascriptExecutor executor;
    @Getter
    private WebDriverWait webWait;
    @Getter
    private WebDriver driver;

    public void start(Options options) {
        try {
            driver = new ChromeDriver(getChromeOptions());
            webWait = new WebDriverWait(driver, 15);
            driver.manage().timeouts().setScriptTimeout(3, TimeUnit.SECONDS);
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
            executor = (JavascriptExecutor) driver;
            run(options);
            System.out.println("Runner execution finished successfully");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    private void run(Options options) throws IOException {
        String url = options.formatFullUrl();
        driver.navigate().to(url);
        By searchResults = By.id("search_results");
        webWait.until(ExpectedConditions.presenceOfElementLocated(searchResults));
        String js = "return $('#search_results td > a[itemprop=url]').parent().next().next().find('a').attr('href');";
        String link = (String) executor.executeScript(js);
        File zip = download(link, options);
        File destinationDir = Paths.get(options.getTemporaryDirectory(), options.getSeries() + "_" + options.getSeason() + "_" + options.getEpisode()).toFile();
        ZipUtils.unzip(zip, destinationDir);
        zip.delete();
        boolean hasPolish = false;
        File dir = new File(options.getDownloadDirectory(),
                String.format("%s_s%02de%02d", options.getSeries(), options.getSeason(), options.getEpisode()));
        for (File file : destinationDir.listFiles()) {
            String prefix = "_";
            if ("polish".contains(file.getName())) {
                prefix = "pl" + prefix;
                hasPolish = true;
            } else if ("english".contains(file.getName())) {
                prefix = "en" + prefix;
            }
            for (File sub : file.listFiles()) {
                dir.mkdirs();
                Files.copy(sub.toPath(), Paths.get(dir.getAbsolutePath(), prefix + sub.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.walk(destinationDir.toPath())
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2))
                .forEach(File::delete);
        System.out.println("Files saved to " + dir.getAbsolutePath());
        if (hasPolish) {
            System.out.println("Detected Polish subtitles - finishing");
        } else {
            translateSubs();
        }
    }

    private void translateSubs() {

    }

    private File download(String link, Options options) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(options.getBaseUrl() + link).build();
        Response response = client.newCall(request).execute();
        Path path = Paths.get(options.getTemporaryDirectory(), options.getSeries() + "_" + options.getSeason() + "_" + options.getEpisode() + ".zip");
        BufferedSink sink = Okio.buffer(Okio.sink(path));
        sink.writeAll(response.body().source());
        sink.close();
        return path.toFile();
    }

    private ChromeOptions getChromeOptions() {
        Configuration c = Configuration.get();
        DesiredCapabilities dc = new DesiredCapabilities();
        dc.setJavascriptEnabled(false);
        ChromeOptions options = new ChromeOptions();
        options.setBinary(c.getChromeExePath());
        options.setCapability("applicationCacheEnabled", true);
        for (String s : c.getChromeExtensionsPaths()) {
            options.addExtensions(new File(s));
        }
        options.merge(dc);
        return options;
    }
}
