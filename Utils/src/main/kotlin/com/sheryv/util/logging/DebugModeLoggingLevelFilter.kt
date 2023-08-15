package com.sheryv.util.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.sheryv.util.CoreUtils


class DebugModeLoggingLevelFilter : Filter<ILoggingEvent>() {
  
  var whenEnabled: Level = Level.DEBUG
  var whenDisabled: Level = Level.INFO
  
  private val debug: Boolean by lazy { CoreUtils.parseBoolean(System.getProperty("debug")) }
  
  override fun decide(event: ILoggingEvent): FilterReply {
    if (!isStarted) {
      return FilterReply.NEUTRAL
    }
    
    val level = if (debug) {
      whenEnabled
    } else {
      whenDisabled
    }
    
    return if (event.level.isGreaterOrEqual(level)) {
      FilterReply.NEUTRAL
    } else {
      FilterReply.DENY
    }
  }
}


