package com.sheryv.tools.websitescraper.utils

//import kotlinx.coroutines.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sheryv.util.logging.LoggingUtils
import kotlinx.coroutines.*
import org.slf4j.Logger
import java.text.DecimalFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField
import kotlin.math.log10
import kotlin.math.pow

object Utils {
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
  
  val jsonMapper by lazy { createJsonMapper() }
  
  fun createJsonMapper(types: Map<String, Class<*>> = emptyMap()): ObjectMapper {
    val map = ObjectMapper()
    map.configure(SerializationFeature.INDENT_OUTPUT, true)
    map.registerModule(KotlinModule.Builder().build())
    map.registerModule(JavaTimeModule())
    map.dateFormat = StdDateFormat()
    map.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    map.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    map.registerSubtypes(*types.map { NamedType(it.value, it.key) }.toTypedArray())
    return map
  }
  
  inline fun <reified T> deserializeList(list: List<*>?): List<T>? {
    if (list == null) return null
    val type = object : TypeReference<List<T>>() {}
    return jsonMapper.convertValue(list, type)
  }
  
  inline fun <reified T> jacksonTypeRef(): TypeReference<T> = object : TypeReference<T>() {}
}

inline fun <reified T> T.lg(clazz: Class<*> = T::class.java): Logger {
  return LoggingUtils.getLogger(clazz)
}

inline fun Any.lg(name: String): Logger {
  return LoggingUtils.getLogger(name)
}


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

