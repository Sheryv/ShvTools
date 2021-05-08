package com.sheryv.util.logging;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleUtils {
  private static final Pattern PATTERN_COLOR = Pattern.compile("(&\\d\\d?&)");
  
  private ConsoleUtils() {
  }
  
  public static String parseAndReplaceWithColors(String text) {
    Matcher matcher = PATTERN_COLOR.matcher(text);
    String result = text+"";
    while (matcher.find()) {
      String group = matcher.group();
      String substring = text.substring(matcher.start(), matcher.end());
      String code = group.substring(1, group.length() - 1);
      code = COLORS.get(code);
      code = code != null ? code : "";
      result = result.replace(substring, code);
    }
    return result;
//    return matcher.replaceAll(matchResult -> {
//      String group = matchResult.group();
//      String code = group.substring(1, group.length() - 1);
//      code = COLORS.get(code);
//      return code != null ? code : "";
//    });
  }
  
  private static final Map<String, String> COLORS = new HashMap<>();
  
  
  static final String RESET = "\033[0m";  // Text Reset
  
  static final String BLACK = "\033[0;30m";   // BLACK
  static final String RED = "\033[0;31m";     // RED
  static final String GREEN = "\033[0;32m";   // GREEN
  //   static final String YELLOW = "\033[0;33m";  // YELLOW
  static final String BLUE = "\033[0;34m";    // BLUE
  //   static final String PURPLE = "\033[0;35m";  // PURPLE
  static final String CYAN = "\033[0;36m";    // CYAN
  static final String WHITE = "\033[0;37m";   // WHITE
  
  
  static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
  static final String RED_BRIGHT = "\033[0;91m";    // RED
  static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
  static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
  static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
  static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
  static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
  static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE
  
  static {
    COLORS.put("0", RESET);
    COLORS.put("1", GREEN);
    COLORS.put("2", CYAN);
    COLORS.put("3", RED);
    COLORS.put("4", BLUE);
    COLORS.put("5", WHITE);
    COLORS.put("6", BLACK);
  }
}
