package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.tools.movielinkgripper.Format;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VodGoProvider extends VideoProvider {

    public static final String BASE_URL = "https://vodgo.pl";
    private static final String ROW_SELECTOR = ".mt-3.p-0 .tab-content > .tab-pane > ul > li > div";

    public VodGoProvider(String series, int season, String allEpisodesLinkPart) {
        super(series, season, allEpisodesLinkPart);
    }

    @Override
    public String getMainLang() {
        return "pl";
    }

    @Override
    public String getProviderUrl() {
        return BASE_URL;
    }

    @Override
    public List<String> findEpisodesNames() {
        return null;
    }

    @Override
    public List<String> findEpisodesLinks(String serverIndex) {
        return null;
    }

    @Override
    public List<Item> findEpisodesItems(String serverIndex) {
        List<WebElement> until = getGripper().getWebWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("s" + season)));
        String js = "return $('#s"+season+" > ul > li > div > div.col-10.col-md-11 > a').map((e, a) => {return {e:$(a).text(), u:$(a).attr('href')};}).get();";
        List<Map<String, String>> mapList = gripper.executeScriptFetchList(js);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < mapList.size(); i++) {
            Map<String, String> map = mapList.get(i);
            String name = map.get("e").trim();
            String link = map.get("u");
            items.add(new Item(link, name, i + 1));
        }
        return items;
    }

    @Override
    public List<Hosting> loadItemDataFromSummaryPageAndGetVideoLinks(Item item) {
        String js = "return $('" + ROW_SELECTOR + "').map((e,a)=>{ return {h:$(a).find('div>button').first().text(), q:$(a).find('div>button').eq(1).text()};}).get();";
        List<Map<String, String>> mapList = gripper.executeScriptFetchList(js);
        List<Hosting> videos = new ArrayList<>();
        for (int i = 0; i < mapList.size(); i++) {
            Map<String, String> map = mapList.get(i);
            String name = map.get("h").trim().toLowerCase();
            String format = map.get("q").trim().toLowerCase();
            videos.add(new Hosting(name, EpisodesTypes.UNKNOWN, new Format().setQuality(format), i, null));
        }

        return videos;
    }

    @Override
    public void openVideoPage(Item item, Hosting videoLink) {
        gripper.getDriver().navigate().refresh();
        gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(ROW_SELECTOR + " > div > form")));
        String js = "$('" + ROW_SELECTOR + " > div > form a').eq(" + videoLink.getIndex() + ").click();";
        gripper.executeScript(js);
    }

    @Nullable
    @Override
    public String findLoadedVideoDownloadUrl(Item item, Hosting hosting) {
        WebDriver driver = gripper.getDriver();
//        WebElement iframe = driver.findElement(By.cssSelector(".container iframe"));
        WebElement iframe = waitForAttribute("src", By.cssSelector("#player_iframe"));
        driver.switchTo().frame(iframe);
//        By btn = By.cssSelector("button");
//        WebElement btnElement = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(btn));
        return initializeHostingAndGetUrl(hosting);
    }
}
