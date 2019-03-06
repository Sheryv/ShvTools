package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.Gripper;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
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
        if (allEpisodesLinkPart.startsWith("http:")
                || allEpisodesLinkPart.startsWith("http:")
                || allEpisodesLinkPart.startsWith("www"))
            return allEpisodesLinkPart;

        if (!allEpisodesLinkPart.startsWith("/"))
            return getProviderUrl() + "/" + allEpisodesLinkPart;
        else
            return getProviderUrl() + allEpisodesLinkPart;
    }

    public abstract String getMainLang();

    public abstract String getProviderUrl();

    public abstract List<String> findEpisodesNames();

    public abstract List<String> findEpisodesLinks(String serverIndex);

    public abstract List<Item> findEpisodesItems(String serverIndex);

    public abstract void goToEpisodePage(Item item);

    public abstract List<Hosting> loadItemDataFromSummaryPageAndGetVideoLinks(Item item);

    public abstract void openVideoPage(Item item, String videoLink);

    @Nullable
    public abstract String findLoadedVideoDownloadUrl(Item item);

}
