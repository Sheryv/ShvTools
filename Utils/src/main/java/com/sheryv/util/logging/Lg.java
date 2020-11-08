package com.sheryv.util.logging;

import org.slf4j.Logger;

public class Lg extends LoggingUtils {
  
  public static Logger colored(Class<?> clazz) {
    return new ColoredLogger(clazz);
  }
  
  public static Logger colored(String name) {
    return new ColoredLogger(name);
  }
  
  public static void printColored(String text) {
    System.out.println(ConsoleUtils.parseAndReplaceWithColors(text));
  }
  
  private static class ColoredLogger extends LoggerMessageDecorator {
    private final Logger logger;
    
    public ColoredLogger(Class<?> clazz) {
      this.logger = LoggingUtils.getLogger(clazz);
    }
    
    public ColoredLogger(String name) {
      this.logger = LoggingUtils.getLogger(name);
    }
    
    
    @Override
    public Logger getLogger() {
      return logger;
    }
    
    @Override
    protected String transformMessage(String message) {
      return ConsoleUtils.parseAndReplaceWithColors(message);
    }
  }
}
