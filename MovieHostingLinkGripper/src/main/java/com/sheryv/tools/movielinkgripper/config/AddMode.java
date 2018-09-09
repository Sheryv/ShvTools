package com.sheryv.tools.movielinkgripper.config;

import lombok.Getter;

@Getter
public class AddMode extends AbstractMode {

    public static final String NAME = "Add";

    public AddMode() {
        super(NAME, "a");
    }


    @Override
    public void execute(Configuration configuration) throws Exception {
        throw new IllegalStateException("Not implemented");
    }
}
