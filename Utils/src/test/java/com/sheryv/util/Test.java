package com.sheryv.util;

import com.sheryv.util.logging.ConsoleUtils;
import com.sheryv.util.logging.Lg;

public class Test {
  public static void main(String[] args) {
    System.out.println("&3&Color&0& after " + ConsoleUtils.parseAndReplaceWithColors("&3&Color&0&"));
    for (int i = 1; i < 7; i++) {
      Lg.printColored(">> &" + i + "&Color&0& and no color");
    }
  }
}
