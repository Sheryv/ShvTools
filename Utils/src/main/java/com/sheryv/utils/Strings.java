package com.sheryv.utils;

import javax.annotation.Nullable;

public class Strings {
    private Strings() {
    }

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

}
