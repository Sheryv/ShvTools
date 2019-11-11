package com.sheryv.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.StreamSupport;

public class VersionUtils {

    public static String loadVersionByModuleName(String moduleName) {
        InputStream resourceAsStream = VersionUtils.class.getClassLoader().getResourceAsStream(moduleName + ".txt");
        try {
            return new String(resourceAsStream.readAllBytes(), FileUtils.getCharset());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
