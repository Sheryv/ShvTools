package com.sheryv.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.HashMap;
import java.util.Map;

public class LoggingUtils {

  //  LimitedQueue<String> history = LimitedQueue<String>(500)
//  EventBus eventBus = EventBus.builder().logger(org.greenrobot.eventbus.Logger.JavaLogger(EventBus::class.java.name)).build()
  private static final Marker HIDDEN_MARKER = MarkerFactory.getMarker("hid");
  private static final Map<String, Logger> LOGGERS = new HashMap<>();
  
  public static Logger getLogger(Class<?> clazz) {
    Logger logger;
    if (!LOGGERS.containsKey(clazz.getName())) {
      logger = LoggerFactory.getLogger(clazz);
      LOGGERS.put(clazz.getName(), logger);
    } else {
      logger = LOGGERS.get(clazz.getName());
    }
    return logger;
  }
  
  public static Logger getLogger(String name) {
    Logger logger;
    if (!LOGGERS.containsKey(name)) {
      logger = LoggerFactory.getLogger(name);
      LOGGERS.put(name, logger);
    } else {
      logger = LOGGERS.get(name);
    }
    return logger;
  }
  
  
  public static void hidden(String msg, Class<?> clazz) {
    getLogger(clazz).debug(HIDDEN_MARKER, msg);
  }

//  public void appendHistory(String log) {
//    history.add(log)
//  }
}
