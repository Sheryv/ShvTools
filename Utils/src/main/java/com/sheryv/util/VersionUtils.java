package com.sheryv.util;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.stream.StreamSupport;

public class VersionUtils {
  
  public static String loadVersionByModuleName(String moduleName) {
    InputStream resourceAsStream = VersionUtils.class.getClassLoader().getResourceAsStream(moduleName + ".txt");
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
      StringBuilder builder = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        builder.append(line).append('\n');
        line = reader.readLine();
      }
      return builder.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }
}
