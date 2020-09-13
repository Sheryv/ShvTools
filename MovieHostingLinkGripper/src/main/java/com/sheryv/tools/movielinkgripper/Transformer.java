package com.sheryv.tools.movielinkgripper;

import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.tools.movielinkgripper.provider.*;
import com.sheryv.util.FileUtils;
import com.sheryv.util.SerialisationUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class Transformer {

    public static Series loadSeries(String json) throws IOException {
        return SerialisationUtils.fromJson(json, Series.class);
    }

    public static void sendToIDM(Series series, Configuration configuration) {
        for (Episode episode : series.getEpisodes()) {
            if (episode.getError() == 0) {
                Gripper.addToIDM(series, episode, configuration);
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
        for (Map.Entry<String, Creator> entry : PROVIDERS.entrySet()) {
            if (entry.getKey().contains(provider)) {
                return entry.getValue().create(seriesName, season, relativeLink);
            }
        }
        return null;
    }

    public static final Map<String, Creator> PROVIDERS = new HashMap<>();

    static {
        PROVIDERS.put(appendUrl("alltube ", AlltubeProvider.BASE_URL), AlltubeProvider::new);
        PROVIDERS.put(appendUrl("zerion", ZerionProvider.BASE_URL), ZerionProvider::new);
        PROVIDERS.put(appendUrl("fmovies", FMoviesProvider.BASE_URL), FMoviesProvider::new);
        PROVIDERS.put(appendUrl("fili", FiliProvider.BASE_URL), FiliProvider::new);
        PROVIDERS.put(appendUrl("vodgo", VodGoProvider.BASE_URL), VodGoProvider::new);
        PROVIDERS.put(appendUrl("streamlord", StremalordProvider.BASE_URL), StremalordProvider::new);
    }

    private static String appendUrl(String name, String url) {
        return name + " [" + url + "]";
    }

    interface Creator {
        VideoProvider create(String seriesName, int season, String relativeLink);
    }
}
