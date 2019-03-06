package com.sheryv.utils;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Strings {
    private Strings() {
    }

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    public static String getFullStackTrace(Throwable throwable) {
        StringWriter out = new StringWriter();
        throwable.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
