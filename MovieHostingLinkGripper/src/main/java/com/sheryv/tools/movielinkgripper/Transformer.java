package com.sheryv.tools.movielinkgripper;

import com.sheryv.tools.movielinkgripper.provider.AlltubeProvider;
import com.sheryv.tools.movielinkgripper.provider.FMoviesProvider;
import com.sheryv.tools.movielinkgripper.provider.VideoProvider;
import com.sheryv.utils.FileUtils;
import com.sheryv.utils.SerialisationUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class Transformer {

    public static Series loadSeries(String json) throws IOException {
        return SerialisationUtils.fromJson(json, Series.class);
    }

    public static void sendToIDM(Series series) {
        for (Episode episode : series.getEpisodes()) {
            if (episode.getError() == 0) {
                Gripper.addToIDM(series, episode);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Err sleeping", e);
                }
            } else {
                System.out.println("No download link for " + episode);
            }
        }
    }


    public static boolean replaceLink(String path, int num, String newUrl) throws IOException {
        Path p = Paths.get(path);
        String s = FileUtils.readFileInMemory(p);
        Series series = loadSeries(s);
        List<Episode> episodes = series.getEpisodes();
        for (int i = 0; i < episodes.size(); i++) {
            Episode episode = episodes.get(i);
            if (episode.getN() == num) {
                episodes.set(i, new Episode(episode.getPage(), episode.getName(), episode.getN(),
                        newUrl, EpisodesTypes.LECTOR));
                FileUtils.saveFile(SerialisationUtils.toJsonPretty(series), p);
                return true;
            }
        }
        return false;
    }

    public static VideoProvider createProvider(String provider, String seriesName, int season, String relativeLink) {
        if (("alltube" + AlltubeProvider.BASE_URL).contains(provider)) {
            return new AlltubeProvider(seriesName, season, relativeLink);
        } else if (("fmovies" + FMoviesProvider.BASE_URL).contains(provider)) {
            return new FMoviesProvider(seriesName, season, relativeLink);
        }
        return null;
    }
}
