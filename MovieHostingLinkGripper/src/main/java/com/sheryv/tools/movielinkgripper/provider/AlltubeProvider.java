package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AlltubeProvider extends VideoProvider {

    private static final String[] HOSTINGS = new String[]{"streamango", "streamcherry", "vidoza", "openload"};
    private static final String[] ADDITIONAL_HOSTINGS = new String[]{};
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
            String name = parts[1];
            items.add(new Item(u, name, num));
        }
        return items;
    }

    @Override
    public void goToEpisodePage(Item item) {
        gripper.getDriver().navigate().to(item.getLink());
    }

    @Override
    public List<Hosting> loadItemDataFromSummaryPageAndGetVideoLinks(Item item) {
        List<String> hostings = new ArrayList<>(Arrays.asList(HOSTINGS));
        if (gripper.getOptions().isUseMoreProviders()) {
            hostings.addAll(Arrays.asList(ADDITIONAL_HOSTINGS));
        }
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
        for (String link : links) {
            String[] parts = link.split("\\|\\|\\|");
            String hosting = parts[0].trim().toLowerCase();
            if (hostings.contains(hosting)) {
                videos.add(new Hosting(hosting, parseType(parts[1].trim()), parts[2].trim(), parts[3].trim()));
            }
        }
        if (videos.isEmpty())
            System.out.printf("No hosting found for: E%02d %s | %s%n", item.getNum(), item.getName(), item.getLink());
        return videos;
    }

    @Override
    public void openVideoPage(Item item, String videoLink) {
        WebDriver driver = gripper.getDriver();
        driver.navigate().to(videoLink);
    }

    @Override
    public String findLoadedVideoDownloadUrl(Item item) {
        WebDriver driver = gripper.getDriver();
//        WebElement iframe = driver.findElement(By.cssSelector(".container iframe"));
        WebElement iframe = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".container iframe")));
        driver.switchTo().frame(iframe);
//        By btn = By.cssSelector("button");
//        WebElement btnElement = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(btn));
        gripper.executeScript("return $('#videooverlay').click();");
        gripper.executeScript("return $('div button').eq(0).click();");
//        btnElement.click();
        By byVideo = By.cssSelector("video:not(.hidden)");
        WebElement video = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(byVideo));
        return video.getAttribute("src");
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
