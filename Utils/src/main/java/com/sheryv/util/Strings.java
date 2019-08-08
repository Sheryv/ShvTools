package com.sheryv.util;

import org.apache.commons.text.StringSubstitutor;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

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

    public static StringSubstitutor getTemplater(Map<String, Object> values){
        return new StringSubstitutor(values, "{{", "}}");
    }
    
    public static String fillTemplate(String template, Map<String, Object> values){
        return getTemplater(values).replace(template);
    }
}
