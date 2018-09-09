package com.sheryv.tools.movielinkgripper.provider;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AlltubeProvider extends VideoProvider {

    private static final String[] HOSTINGS = new String[]{"streamango", "streamcherry", "vidoza"};
    private static final String[] ADDITIONAL_HOSTINGS = new String[]{};
    public static final String BASE_URL = "https://alltube.tv";

    public AlltubeProvider(String series, int seriesNum, String allEpisodesLinkPart) {
        super(series, seriesNum, allEpisodesLinkPart);
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
        String s = "return $('.episode-list li a').filter(function(i, b){return $(this).text().search('s0" + season + "') >= 0; }).map(function(e){return {e: $(this).text(), u: $(this).attr('href')};  }).get();";
        Object o = gripper.executeScript(s);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> mapList = (List<Map<String, String>>) o;
        List<Item> items = new ArrayList<>();
        for (Map<String, String> map : mapList) {
            String e = map.get("e");
            String u = map.get("u");
            String[] parts = e.split("\\] ");
            int num = Integer.parseInt(parts[0].substring(parts[0].indexOf('e') + 1, parts[0].length()));
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
    public void startVideoLoading(Item item) {

    }

    @Override
    public String findDownloadLink(Item item) {
        List<String> hostings = new ArrayList<>(Arrays.asList(HOSTINGS));
        if (gripper.getOptions().isUseMoreProviders()) {
            hostings.addAll(Arrays.asList(ADDITIONAL_HOSTINGS));
        }
        for (String hosting : hostings) {
            String js = "return $(\"#links-container table tr td:contains('" + hosting + "')\").parent().find('a.watch').map(function(a,e){return e.getAttribute('href');}).get();";
            @SuppressWarnings("unchecked")
            List<String> links = (List<String>) gripper.executeScript(js);
            if (links.size() > 0) {
                String v = links.get(0);
                WebDriver driver = gripper.getDriver();
                driver.navigate().to(v);
                WebElement iframe = driver.findElement(By.cssSelector(".container iframe"));
                driver.switchTo().frame(iframe);
                By btn = By.cssSelector("button");
                gripper.executeScript("return $('body div button').click();");
                By byVideo = By.cssSelector("video");
                WebElement video = gripper.getWebWait().until(ExpectedConditions.presenceOfElementLocated(byVideo));

                return video.getAttribute("src");
            }
        }
        System.out.printf("No hosting found for: E%02d %s | %s%n", item.getNum(), item.getName(), item.getLink());
        return null;
    }
}
