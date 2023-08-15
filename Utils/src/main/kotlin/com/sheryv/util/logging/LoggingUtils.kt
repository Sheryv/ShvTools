@file:Suppress("NOTHING_TO_INLINE")

package com.sheryv.util.logging

import ch.qos.logback.classic.LoggerContext
import com.sheryv.util.LimitedQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.lang.management.ManagementFactory


object LoggingUtils {
  val hiddenLoggerTag = "HIDDEN"
  private val loggers: MutableMap<String, Logger> = HashMap()
  
  //  const val COLORED_LOGGER_TAG = "COLORED"
  private var logHistory: MutableCollection<String>? = null
    private set
  
  public val logHistoryEnabled
    get() = logHistory != null
  
  @JvmStatic
  fun getLogger(clazz: Class<*>): Logger {
    
    return if (!loggers.containsKey(clazz.getName())) {
      val logger = LoggerFactory.getLogger(clazz)
      loggers[clazz.getName()] = logger
      logger
    } else {
      loggers[clazz.getName()]!!
    }
  }
  
  @JvmStatic
  fun getLogger(name: String): Logger {
    return if (!loggers.containsKey(name)) {
      val logger = LoggerFactory.getLogger(name)
      loggers[name] = logger
      logger
    } else {
      loggers[name]!!
    }
  }
  
  
  fun enableLogsHistory(size: Int = 50) {
    logHistory = LimitedQueue(size)
  }
  
  
  fun appendHistoryLog(log: String) {
    synchronized(this) {
      logHistory!!.add(log)
    }
  }
  
  fun getHistory(): List<String> {
    return synchronized(this) {
      return@synchronized logHistory!!.toList()
    }
  }
}

val log: Logger by lazy {
  val start = System.currentTimeMillis()
  val factory = LoggerFactory.getILoggerFactory() as LoggerContext
  val logger = factory.getLogger(LoggingUtils::class.java)
  if (LoggingUtils.logHistoryEnabled) {
    logger.addAppender(LoggingListener())
  }
  logger.debug("Initialized logging at {} in {} ms", ManagementFactory.getRuntimeMXBean().uptime, System.currentTimeMillis() - start)
  logger
}

inline fun Logger.hidden(msg: String, vararg arguments: Any?) {
  log.debug(MarkerFactory.getMarker(LoggingUtils.hiddenLoggerTag), msg, arguments)
}

inline fun Logger.colored(msg: String, vararg arguments: Any?) = log.debug(ConsoleUtils.parseAndReplaceWithColors(msg), arguments)

fun Logger.systemPrintColored(text: String) = println(ConsoleUtils.parseAndReplaceWithColors(text))


