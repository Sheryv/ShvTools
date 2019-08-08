package com.sheryv.tools.movielinkgripper.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HostingConfig implements Serializable {
    private String searchName;
    private int priority;
    private String code;
    private boolean enabled;
}
