package com.sheryv.tools.movielinkgripper;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Format {
    private String rating;
    private String quality;
}
