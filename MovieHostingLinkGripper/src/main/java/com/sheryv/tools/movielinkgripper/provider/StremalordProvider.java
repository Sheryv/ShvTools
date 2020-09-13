package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.tools.movielinkgripper.Format;
import com.sheryv.tools.movielinkgripper.config.HostingConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StremalordProvider extends VideoProvider {

    public static final String BASE_URL = "http://www.streamlord.com/";

    public StremalordProvider(String series, int season, String allEpisodesLinkPart) {
        super(series, season, allEpisodesLinkPart);
    }

    @Override
    public String getMainLang() {
        return "en";
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
        List<WebElement> until = getGripper().getWebWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("season-wrapper")));
        String js = "return $('#season-wrapper ul').eq($('#season-wrapper ul').size()-" + season + ").children('li').map((i,e)=>{ return {e:$(e).children('.head').text().substring($(e).find('.head > span').text().length), u:$(e).find('.content a').attr('href')} }).get();";
        List<Map<String, String>> mapList = gripper.executeScriptFetchList(js);
        Collections.reverse(mapList);
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
//        String js = "return [...document.querySelectorAll('.video-list tr')].filter(v=>v.childNodes[0].nodeName == 'TD').map(v=>{return {h: v.childNodes[0].textContent, q: v.childNodes[1].textContent}})";
//        List<Map<String, String>> mapList = gripper.executeScriptFetchList(js);
        //        for (int i = 0; i < mapList.size(); i++) {
//            Map<String, String> map = mapList.get(i);
//            String name = map.get("h").trim().toLowerCase();
//            String format = map.get("q").trim().toLowerCase();
//            videos.add(new Hosting(name, EpisodesTypes.UNKNOWN, new Format().setQuality(format), i, null));
//        }

        return Collections.singletonList(new Hosting(Hosting.UNKNOWN_NAME, EpisodesTypes.ORIGIN, null, 0, null));
    }

    @Override
    public void openVideoPage(Item item, Hosting videoLink) {
        String js = "$('#parall').click()";
        gripper.executeScript(js);
    }

    @Nullable
    @Override
    public String findLoadedVideoDownloadUrl(Item item, Hosting hosting) {
        WebElement video = waitForAttribute("src", By.cssSelector(".videostream video"));
        return video.getAttribute("src");
    }
}
