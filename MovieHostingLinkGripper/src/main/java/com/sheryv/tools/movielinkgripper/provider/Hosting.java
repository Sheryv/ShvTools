package com.sheryv.tools.movielinkgripper.provider;

import com.sheryv.tools.movielinkgripper.EpisodesTypes;
import com.sheryv.tools.movielinkgripper.Format;
import lombok.Data;

import java.io.Serializable;

@Data
public class Hosting {
    private final String name;
    private final EpisodesTypes type;
    private final Format format;
    private final int index;
    private final String videoLink;
}
