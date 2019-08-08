package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.Gripper;
import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.util.Strings;
import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;

@Getter
public abstract class VideoProvider {

    protected final String series;
    protected final int season;
    //starts with slash
    protected final String allEpisodesLinkPart;
    @Setter
    protected Gripper gripper;

    public VideoProvider(String series, int season, String allEpisodesLinkPart) {
        this.series = series;
        this.season = season;
        this.allEpisodesLinkPart = allEpisodesLinkPart;
    }

    public String getSeriesLink() {
        return getFullUrl(allEpisodesLinkPart);
    }

    public abstract String getMainLang();

    public abstract String getProviderUrl();

    public abstract List<String> findEpisodesNames();

    public abstract List<String> findEpisodesLinks(String serverIndex);

    public abstract List<Item> findEpisodesItems(String serverIndex);

    public void goToEpisodePage(Item item) {
        gripper.getDriver().navigate().to(getFullUrl(item.getLink()));
    }

    public abstract List<Hosting> loadItemDataFromSummaryPageAndGetVideoLinks(Item item);

    public abstract void openVideoPage(Item item, Hosting videoLink);

    @Nullable
    public abstract String findLoadedVideoDownloadUrl(Item item, Hosting hosting);


    public String getFullUrl(String url) {
        if (url.startsWith("http:")
                || url.startsWith("https:")
                || url.startsWith("www"))
            return url;

        if (!url.startsWith("/"))
            return getProviderUrl() + "/" + url;
        else
            return getProviderUrl() + url;
    }

    protected String initializeHostingAndGetUrl(Hosting hosting) {
        waitTime(500);
        //openload
        gripper.executeScript("let shvbtn = document.querySelector('#videooverlay'); if(shvbtn) shvbtn.click();");
        //streamango
        gripper.executeScript("let shvbtn = document.querySelector('div button:not(.close)'); if(shvbtn) shvbtn.click();");
        //verystream
        gripper.executeScript("let shvbtn = document.querySelector('#videerlay'); if(shvbtn) shvbtn.click();");

        //        btnElement.click();
        waitTime(200);
        By byVideo = By.cssSelector("video:not(.hidden)");
        if (waitForAttribute("src", By.cssSelector("video")) == null) {
            try {
                WebElement captacha = gripper.getWebWait().withTimeout(Duration.ofSeconds(2)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".captcha")));
                if (captacha != null) {
                    Configuration.setPaused(true);
                    do {
                        waitTime(500);
                    } while (Configuration.isPaused());
                }
            } catch (Exception e) {
                System.out.printf("At searching for captcha: %s%n", e.getMessage());
            } finally {
                WebElement inner = gripper.getWebWait().withTimeout(Duration.ofSeconds(2)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("iframe")));
                if (inner != null) {
                    gripper.getDriver().switchTo().frame(inner);
                    System.out.printf("Switched to inner frame in %s [%d]%n", hosting.getName(), hosting.getIndex());
                }
            }
        }
        //openload
        gripper.executeScript("let shvbtn = document.querySelector('#videooverlay'); if(shvbtn) shvbtn.click();");
        //streamango
        gripper.executeScript("let shvbtn = document.querySelector('div button:not(.close)'); if(shvbtn) shvbtn.click();");
        //verystream
        gripper.executeScript("let shvbtn = document.querySelector('#videerlay'); if(shvbtn) shvbtn.click();");
        waitTime(100);
        WebElement video = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(byVideo));
        return video.getAttribute("src");
    }

    protected WebElement waitForAttribute(String attribute, By selector) {
        WebElement element = null;
        try {
            for (int i = 0; i < 100; i++) {
                element = gripper.getWebWait().withTimeout(Duration.ofSeconds(5)).until(ExpectedConditions.presenceOfElementLocated(selector));
                if (!Strings.isNullOrEmpty(element.getAttribute(attribute))) {
                    break;
                }
                element = null;
                waitTime(300);
            }
        } catch (Exception e) {
            System.out.printf("At waiting for '%s' with selector '%s': %s%n", attribute, selector.toString(), e.getMessage());
        }
        return element;
    }

    protected void waitTime(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
