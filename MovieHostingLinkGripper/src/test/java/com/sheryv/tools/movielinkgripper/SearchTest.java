package com.sheryv.tools.movielinkgripper;

import com.sheryv.tools.movielinkgripper.search.TmdbSeason;
import com.sheryv.tools.movielinkgripper.search.TmdbApi;
import com.sheryv.util.SerialisationUtils;
import org.junit.Test;

import java.io.IOException;

public class SearchTest {

    @Test
    public void name() throws IOException {
        TmdbApi api = new TmdbApi();
        String s = api.sendRequest("https://api.themoviedb.org/3/tv/46952/season/1?api_key=fcdd9eafcf41354bd37be41d4b4fa3aa&language=en-US");
        TmdbSeason season = SerialisationUtils.fromJson(s, TmdbSeason.class);
        System.out.println();
    }
}
