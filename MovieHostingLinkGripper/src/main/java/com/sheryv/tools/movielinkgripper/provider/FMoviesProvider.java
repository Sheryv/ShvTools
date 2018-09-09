package com.sheryv.tools.movielinkgripper.provider;

import javafx.util.Pair;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.annotation.Nullable;
import java.util.List;

public class FMoviesProvider extends VideoProvider {

    public static final String BASE_URL = "https://www7.fmovies.se";
    private static final By SEL_IFRAME = By.cssSelector("#player iframe");
    public static final String EPISODES_CSS_SELECTOR = ".widget-body .items .item span";
    private static final By SEL_EPISODES = By.cssSelector(EPISODES_CSS_SELECTOR);
    private static final By SEL_PLAYER = By.cssSelector("video");
    private static final String SRC_ATTR = "src";

    public FMoviesProvider(String series, int seriesNum, String allEpisodesLinkPart) {
        super(series, seriesNum, allEpisodesLinkPart);
    }

    @Override
    public String getProviderUrl() {
        return BASE_URL;
    }

    @Override
    public List<String> findEpisodesNames() {
        final String j = String.format("return $('%s').map(function(e) { \n" +
                "return $(this).text();\n" +
                "}).get();", EPISODES_CSS_SELECTOR);
        return gripper.executeScriptFetchList(j);
    }

    @Override
    public List<String> findEpisodesLinks(@Nullable String serverIndex) {
        if (serverIndex == null) serverIndex = "0";
        final String j = String.format("var i = 0; " +
                "return $($('.server.row')[%s]).find('li a').map(function(e){ i++;" +
                "return i+'|'+$(this).attr('href');" +
                "}).get();", serverIndex);
        return gripper.executeScriptFetchList(j);
    }

    @Override
    public List<Item> findEpisodesItems(@Nullable String serverIndex) {
        List<String> episodes = findEpisodesNames();
        List<String> links = findEpisodesLinks(serverIndex);
        return gripper.calculateEpisodesItemsWithNumSeparator(links, episodes, link -> {
            var s = link.split("\\|");
            int n = Integer.parseInt(s[0]);
            return new Pair<>(n, s[1]);
        }, this);
    }

    @Override
    public void goToEpisodePage(Item item) {
        gripper.beginStopLoading();
        gripper.getDriver().navigate().to(item.getLink());
        WebElement element = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(SEL_IFRAME));
        gripper.getDriver().switchTo().frame(element);
    }

    @Override
    public void startVideoLoading(Item item) {
        List<WebElement> elements = gripper.getDriver().findElements(SEL_PLAYER);
        if (elements.size() == 0) {
//                WebElement body_div = element;
            gripper.executeScript("return $('body div input#confirm').click();");
//                WebElement body_div = driver.findElement(By.cssSelector("body div input#confirm"));
//                Actions a = new Actions(driver);
//                a.moveToElement(body_div);
//                a.click(body_div);
//                a.build().perform();
        }
    }

    @Override
    public String findDownloadLink(Item item) {
        WebElement video = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(SEL_PLAYER));
        return video.getAttribute(SRC_ATTR);
    }
}
