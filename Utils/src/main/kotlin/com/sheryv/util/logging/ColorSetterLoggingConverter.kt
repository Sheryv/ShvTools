package com.sheryv.util.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter

class ColorSetterLoggingConverter : CompositeConverter<ILoggingEvent>() {
  
  override fun transform(event: ILoggingEvent, s: String): String {
    val color = firstOption?.let { ConsoleUtils.COLORS_BY_NAMES[it] }
    
    return if (color != null) {
      val sb = StringBuilder()
      sb.append(color)
      sb.append(s)
      sb.append(ConsoleUtils.DEFAULT_WITH_RESET)
      sb.toString()
    } else {
      s
    }
  }
}
