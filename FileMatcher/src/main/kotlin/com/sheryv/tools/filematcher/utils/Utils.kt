package com.sheryv.tools.filematcher.utils

import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import com.sheryv.tools.filematcher.model.event.ShvEvent
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField


fun inBackground(start: CoroutineStart = CoroutineStart.DEFAULT,
                 block: suspend CoroutineScope.() -> Unit): Job {
  return GlobalScope.launch(Dispatchers.IO, start, block)
}

fun inViewThread(start: CoroutineStart = CoroutineStart.DEFAULT,
                 block: suspend CoroutineScope.() -> Unit): Job {
  return GlobalScope.launch(Dispatchers.Main, start, block)
}

inline fun <reified T, R> T.timeLog(name: String, repeats: Int = 1, noinline b: () -> R): R {
  return Hashing.time(name, T::class.java, repeats, b)
}


fun Boolean.toEnglishWord(): String {
  return if (this) "Yes" else "No"
}

object Utils {
  private var DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendValue(ChronoField.HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .appendLiteral(' ')
      .appendValue(ChronoField.DAY_OF_MONTH, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
      .toFormatter()

/*  private var LONG_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendValue(ChronoField.HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
      .appendLiteral(' ')
      .appendValue(ChronoField.DAY_OF_MONTH, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
      .toFormatter()*/
  
  fun dateFormat(millis: Long): String {
    if (millis <= 0) {
      return "Never"
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DATE_TIME_FORMAT)
  }
  
  fun dateFormat(date: OffsetDateTime?): String {
    if (date == null) {
      return "-"
    }
    return date.format(DATE_TIME_FORMAT)
  }
  
  fun dateNow(): String {
    return now().format(DATE_TIME_FORMAT)
  }
  
  
  fun now(): OffsetDateTime {
    return OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).withNano(0)
  }
}

object LoggingUtils {
  
  //  val history = LimitedQueue<String>(500)
  val hiddenMarker = MarkerFactory.getMarker("hid")
  private val loggers = mutableMapOf<String, Logger>()
  val eventBus: EventBus = EventBus.builder().logger(org.greenrobot.eventbus.Logger.JavaLogger(EventBus::class.java.name)).build()
  
  fun getLogger(clazz: Class<*>): Logger {
    return if (!loggers.contains(clazz.name)) {
      val logger = LoggerFactory.getLogger(clazz)
      loggers[clazz.name] = logger
      logger
    } else loggers[clazz.name]!!
  }
  
  fun getLogger(name: String): Logger {
    return if (!loggers.contains(name)) {
      val logger = LoggerFactory.getLogger(name)
      loggers[name] = logger
      logger
    } else loggers[name]!!
  }
  
  inline fun <reified T> hidden(msg: String) {
    getLogger(T::class.java).debug(hiddenMarker, msg)
  }

//  fun appendHistory(log: String) {
//    history.add(log)
//  }
}

inline fun <reified T> T.lg(clazz: Class<*> = T::class.java): Logger {
  return LoggingUtils.getLogger(clazz)
}

inline fun Any.lg(name: String): Logger {
  return LoggingUtils.getLogger(name)
}


fun eventsAttach(receiver: Any) {
  LoggingUtils.eventBus.register(receiver)
  LoggingUtils.getLogger(receiver::class.java).debug("Registered event bus to ${receiver::class.simpleName}")
}

fun eventsDetach(receiver: Any) {
  if (LoggingUtils.eventBus.isRegistered(receiver)) {
    LoggingUtils.eventBus.unregister(receiver)
    LoggingUtils.getLogger(receiver::class.java).debug("Unregistered event bus from ${receiver::class.simpleName}")
  } else {
    LoggingUtils.getLogger(receiver::class.java).warn("Trying to unregister event listener that was not registered")
  }
}

fun postEvent(event: ShvEvent) {
  LoggingUtils.eventBus.post(event)
}