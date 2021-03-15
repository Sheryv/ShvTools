package com.sheryv.tools.movielinkgripper.search;

import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.util.SerialisationUtils;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TmdbApi {

    public String sendRequest(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }


    public List<SearchItem> searchTv(String search) {
        try {
            String json = sendRequest("https://api.themoviedb.org/3/search/tv?api_key=" + Configuration.get().getTmdbKey() + "&language=en-US&query=" + search + "&page=1");
            SearchResult result = SerialisationUtils.fromJson(json, SearchResult.class);
            List<SearchItem> items = result.getResults().stream().sorted(Comparator.comparingDouble(SearchItem::getPopularity).reversed()).collect(Collectors.toList());
            log.info("Found {} items: {}", items.size(), items.stream().map(SearchItem::toString).collect(Collectors.joining("\n", "\n", "\n")));
            
            return items;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public TmdbSeason getTvEpisodes(long id, int season) {
        try {
            String json = sendRequest("https://api.themoviedb.org/3/tv/" + id + "/season/" + season + "?api_key=" + Configuration.get().getTmdbKey() + "&language=en-US");
            return SerialisationUtils.fromJson(json, TmdbSeason.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
