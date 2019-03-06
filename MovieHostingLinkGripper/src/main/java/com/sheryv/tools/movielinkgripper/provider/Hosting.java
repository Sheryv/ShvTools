package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import lombok.Data;

@Data
public class Hosting {
    private final String name;
    private final EpisodesTypes type;
    private final String rating;
    private final String videoLink;
}
