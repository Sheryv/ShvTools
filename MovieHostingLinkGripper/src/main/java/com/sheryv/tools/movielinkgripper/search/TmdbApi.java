package com.sheryv.tools.movielinkgripper.search;

import com.sheryv.tools.movielinkgripper.config.Configuration;
import com.sheryv.util.SerialisationUtils;
import okhttp3.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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


    public Optional<SearchItem> searchTv(String search) {
        try {
            String json = sendRequest("https://api.themoviedb.org/3/search/tv?api_key=" + Configuration.get().getTmdbKey() + "&language=en-US&query=" + search + "&page=1");
            SearchResult result = SerialisationUtils.fromJson(json, SearchResult.class);
            List<SearchItem> items = result.getResults().stream().sorted(Comparator.comparingDouble(SearchItem::getPopularity)).collect(Collectors.toList());
            return Optional.ofNullable(items.get(items.size() - 1));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
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
