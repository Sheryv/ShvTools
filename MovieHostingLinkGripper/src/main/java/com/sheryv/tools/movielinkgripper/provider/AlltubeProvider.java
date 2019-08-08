package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.tools.movielinkgripper.Format;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlltubeProvider extends VideoProvider {

    public static final String BASE_URL = "https://alltube.tv";

    public AlltubeProvider(String series, int seriesNum, String allEpisodesLinkPart) {
        super(series, seriesNum, allEpisodesLinkPart);
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
        List<WebElement> until = getGripper().getWebWait().until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.className("episode-list")));
        String s = "return $('.episode-list li a').filter(function(i, b){return $(this).text().search('s0" + season + "') >= 0; }).map(function(e){return {e: $(this).text(), u: $(this).attr('href')};  }).get();";
        List<Map<String, String>> mapList = gripper.executeScriptFetchList(s);
        List<Item> items = new ArrayList<>();
        for (Map<String, String> map : mapList) {
            String e = map.get("e");
            String u = map.get("u");
            String[] parts = e.split("\\] ");
            int num = Integer.parseInt(parts[0].substring(parts[0].indexOf('e') + 1));
            String name = parts[1].trim();
            items.add(new Item(u, name, num));
        }
        return items;
    }

    @Override
    public List<Hosting> loadItemDataFromSummaryPageAndGetVideoLinks(Item item) {
        String js = "return $('#links-container table tr').map(function(a,e){\n" +
                "  let j = $(e);\n" +
                "  return j.children('td').eq(0).text()+'|||'+\n" +
                "  j.children('td.text-center').eq(0).text()+'|||'+\n" +
                "  j.find('td:last div.rate').text()+'|||'+\n" +
                "  j.find('a.watch').eq(0).attr('href');\n" +
                "}).get();";
        @SuppressWarnings("unchecked")
        List<String> links = (List<String>) gripper.executeScript(js);
        List<Hosting> videos = new ArrayList<>();
        for (int i = 0; i < links.size(); i++) {
            String link = links.get(i);
            String[] parts = link.split("\\|\\|\\|");
            String hosting = parts[0].trim().toLowerCase();
            videos.add(new Hosting(hosting, parseType(parts[1].trim()), new Format().setRating(parts[2].trim()), i, parts[3].trim()));
        }
        return videos;
    }

    @Override
    public void openVideoPage(Item item, Hosting hosting) {
        WebDriver driver = gripper.getDriver();
        driver.navigate().to(hosting.getVideoLink());
    }

    @Override
    public String findLoadedVideoDownloadUrl(Item item, Hosting hosting) {
        WebDriver driver = gripper.getDriver();
//        WebElement iframe = driver.findElement(By.cssSelector(".container iframe"));
        WebElement iframe = waitForAttribute("src", By.cssSelector(".container iframe"));
        driver.switchTo().frame(iframe);
        return initializeHostingAndGetUrl(hosting);
    }

    private EpisodesTypes parseType(String type) {
        type = type.toLowerCase();
        if (type.contains("lektor"))
            return EpisodesTypes.LECTOR;
        if (type.contains("napisy"))
            return EpisodesTypes.SUBS;
        if (type.contains("dubbing"))
            return EpisodesTypes.DUBBING;
        if (type.contains("eng"))
            return EpisodesTypes.ORIGIN;
        return EpisodesTypes.UNKNOWN;
    }

}
