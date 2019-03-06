package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.tools.movielinkgripper.Format;
import com.sheryv.utils.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class Item {
    private final String link;
    private final String name;
    private final int num;

    @Setter
    private Format format;
    @Setter
    private EpisodesTypes type;

    public Item(String link, String name, int num) {
        this(link, name, num, null, EpisodesTypes.UNKNOWN);
    }

    public void updateHosting(Hosting hosting) {
        if (!Strings.isNullOrEmpty(hosting.getRating()))
            setFormat(new Format().setRating(hosting.getRating()));
        setType(hosting.getType());
    }
}
