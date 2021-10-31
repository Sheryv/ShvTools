package com.sheryv.tools.filematcher.utils

import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import com.sheryv.tools.filematcher.model.event.ShvEvent
import com.sheryv.util.logging.LoggingUtils
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
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


fun inBackground(
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): Job {
  return GlobalScope.launch(Dispatchers.IO, start, block)
}

fun inViewThread(
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): Job {
  return GlobalScope.launch(Dispatchers.Main, start, block)
}

inline fun <reified T, R> T.timeLog(name: String, repeats: Int = 1, noinline b: () -> R): R {
  return Hashing.time(name, T::class.java, repeats, b)
}


fun Boolean.toEnglishWord(): String {
  return if (this) "Yes" else "No"
}

object Utils {
  internal val eventBus: EventBus =
    EventBus.builder().logger(org.greenrobot.eventbus.Logger.JavaLogger(EventBus::class.java.name)).build()
  
  private var DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendValue(ChronoField.DAY_OF_MONTH, 2)
    .appendLiteral('-')
    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
    .appendLiteral('-')
    .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
    .appendLiteral(' ')
    .appendValue(ChronoField.HOUR_OF_DAY, 2)
    .appendLiteral(':')
    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
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
  
  fun fileSizeFormat(size: Long?): String {
    if (size == null || size <= 0) return "-"
    val units = arrayOf("B  ", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#")
      .format(size / 1024.0.pow(digitGroups.toDouble()))
      .toString() + " " + units[digitGroups]
  }
}

inline fun <reified T> T.lg(clazz: Class<*> = T::class.java): Logger {
  return LoggingUtils.getLogger(clazz)
}

inline fun Any.lg(name: String): Logger {
  return LoggingUtils.getLogger(name)
}


fun eventsAttach(receiver: Any) {
  Utils.eventBus.register(receiver)
  LoggingUtils.getLogger(receiver::class.java).debug("Registered event bus to ${receiver::class.simpleName}")
}

fun eventsDetach(receiver: Any) {
  if (Utils.eventBus.isRegistered(receiver)) {
    Utils.eventBus.unregister(receiver)
    LoggingUtils.getLogger(receiver::class.java).debug("Unregistered event bus from ${receiver::class.simpleName}")
  } else {
    LoggingUtils.getLogger(receiver::class.java).warn("Trying to unregister event listener that was not registered")
  }
}

fun postEvent(event: ShvEvent) {
  Utils.eventBus.post(event)
}
