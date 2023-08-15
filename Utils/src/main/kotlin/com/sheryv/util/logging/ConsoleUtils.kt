package com.sheryv.util.logging

import java.util.regex.Pattern

object ConsoleUtils {
  private val PATTERN_COLOR = Pattern.compile("(&\\d\\d?&)")
  
  @JvmStatic
  fun parseAndReplaceWithColors(text: String): String {
    val matcher = PATTERN_COLOR.matcher(text)
    var result = text + ""
    while (matcher.find()) {
      val group = matcher.group()
      val substring = text.substring(matcher.start(), matcher.end())
      var code: String? = group.substring(1, group.length - 1)
      code = COLORS[code]
      code = code ?: ""
      result = result.replace(substring, code)
    }
    return result
    //    return matcher.replaceAll(matchResult -> {
//      String group = matchResult.group();
//      String code = group.substring(1, group.length() - 1);
//      code = COLORS.get(code);
//      return code != null ? code : "";
//    });
  }
  
  
  const val RESET = "\u001b[0m" // Text Reset
  
  const val DEFAULT = "\u001b[39m" // Text Reset
  const val DEFAULT_WITH_RESET = "\u001b[0;39m" // Text Reset
  const val BLACK = "\u001b[30m" // BLACK
  const val RED = "\u001b[31m" // RED
  const val GREEN = "\u001b[32m" // GREEN
  const val YELLOW = "\u001B[33m"  // YELLOW
  const val BLUE = "\u001b[34m" // BLUE
  const val PURPLE = "\u001b[35m"  // PURPLE
  const val CYAN = "\u001b[36m" // CYAN
  const val WHITE = "\u001b[37m" // WHITE
  const val BLACK_BRIGHT = "\u001b[90m" // BLACK
  const val RED_BRIGHT = "\u001b[91m" // RED
  const val GREEN_BRIGHT = "\u001b[92m" // GREEN
  const val YELLOW_BRIGHT = "\u001b[93m" // YELLOW
  const val BLUE_BRIGHT = "\u001b[94m" // BLUE
  const val PURPLE_BRIGHT = "\u001b[95m" // PURPLE
  const val CYAN_BRIGHT = "\u001b[96m" // CYAN
  const val WHITE_BRIGHT = "\u001b[97m" // WHITE
  
  fun addBold(colorCode: String) = "\u001B[1;" + colorCode.drop(2)
  
  fun addReset(colorCode: String) = "\u001B[0;" + colorCode.drop(2)
  
  private val COLORS: Map<String, String> = mapOf(
    "0" to RESET,
    "1" to GREEN,
    "2" to CYAN,
    "3" to RED,
    "4" to BLUE,
    "5" to WHITE,
    "6" to BLACK,
  )
  
  val COLORS_BY_NAMES: Map<String, String> = mapOf(
    "DEFAULT" to DEFAULT,
    "BLACK" to BLACK,
    "RED" to RED,
    "GREEN" to GREEN,
    "YELLOW" to YELLOW,
    "BLUE" to BLUE,
    "PURPLE" to PURPLE,
    "CYAN" to CYAN,
    "WHITE" to WHITE,
    "BLACK_BRIGHT" to BLACK_BRIGHT,
    "RED_BRIGHT" to RED_BRIGHT,
    "GREEN_BRIGHT" to GREEN_BRIGHT,
    "YELLOW_BRIGHT" to YELLOW_BRIGHT,
    "BLUE_BRIGHT" to BLUE_BRIGHT,
    "PURPLE_BRIGHT" to PURPLE_BRIGHT,
    "CYAN_BRIGHT" to CYAN_BRIGHT,
    "WHITE_BRIGHT" to WHITE_BRIGHT,
  )
  
}
