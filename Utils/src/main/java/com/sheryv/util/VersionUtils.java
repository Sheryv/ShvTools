package com.sheryv.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VersionUtils {
  
  public static Version loadVersionByModuleName(String moduleName) {
    InputStream resourceAsStream = VersionUtils.class.getClassLoader().getResourceAsStream(moduleName + ".txt");
    long ms = 0;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
      StringBuilder builder = new StringBuilder();
      String version = reader.readLine();
      String time = reader.readLine();
      if (StringUtils.isNotBlank(time)) {
        ms = Long.parseLong(time);
      }
      return new Version(version, ms);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new Version("", ms);
  }
}
