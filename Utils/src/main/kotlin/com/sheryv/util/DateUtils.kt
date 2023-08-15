package com.sheryv.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

object DateUtils {
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
  
  fun shortFormat(millis: Long): String {
    if (millis <= 0) {
      return shortFormat(null)
    }
    return shortFormat(OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))
  }
  
  fun shortFormat(date: OffsetDateTime?): String {
    if (date == null) {
      return "-"
    }
    return date.format(DATE_TIME_FORMAT)
  }
  
  fun nowInShortFormat(): String {
    return now().format(DATE_TIME_FORMAT)
  }
  
  fun now(): OffsetDateTime {
    return OffsetDateTime.now().withNano(0)
  }
  
  fun utc(): OffsetDateTime {
    return OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).withNano(0)
  }
}
