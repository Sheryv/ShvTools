package com.sheryv.util.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

internal class LoggingListener : AppenderBase<ILoggingEvent>() {
  
  private val formatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .toFormatter()
  
  override fun append(e: ILoggingEvent) {
    val internal = e.loggerName.startsWith("com.sheryv")
//    val map = e.mdcPropertyMap
    if (e.markerList.none { it.name == LoggingUtils.hiddenLoggerTag }
      && (e.level.isGreaterOrEqual(Level.WARN)
          || (e.level.isGreaterOrEqual(Level.INFO) && internal)
//          || (e.level.isGreaterOrEqual(Level.DEBUG) && internal && map.containsKey("macro"))
          )
    ) {
      
      val time =
        Instant.ofEpochMilli(e.timeStamp).atZone(ZoneId.systemDefault()).format(formatter)
          .padEnd(23)
      val res = "$time --- (System Event) ${e.level.levelStr[0]}> ${e.formattedMessage}"
      LoggingUtils.appendHistoryLog(res)
    }
  }
}

