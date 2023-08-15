package com.sheryv.util.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter
import ch.qos.logback.core.pattern.color.ANSIConstants

class ColorHighlightingLoggingConverter : CompositeConverter<ILoggingEvent>() {
  private val errorColor = ConsoleUtils.addBold(ConsoleUtils.RED)
  
  override fun transform(event: ILoggingEvent, s: String): String {
    val level = event.level
    val color = when (level.toInt()) {
      Level.ERROR_INT -> errorColor
      Level.WARN_INT -> ConsoleUtils.YELLOW
      Level.INFO_INT -> ConsoleUtils.BLUE_BRIGHT
      Level.DEBUG_INT -> ConsoleUtils.GREEN
      else -> ANSIConstants.DEFAULT_FG
    }
    
    val sb = StringBuilder()
    sb.append(color)
    sb.append(s)
    sb.append(ConsoleUtils.DEFAULT_WITH_RESET)
    return sb.toString()
  }
}
