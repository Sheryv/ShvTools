package com.sheryv.util

import com.sheryv.util.logging.log
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.commons.text.lookup.StringLookupFactory
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil

object Strings {
  private val HEX_ARRAY = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)
  private val SUPPORTED_FORMATTER_CONVERSIONS = listOf('d', 'f', 's', 'o', 'x', 'b')
  fun isNullOrEmpty(string: String?): Boolean {
    return string == null || string.isEmpty()
  }
  
  fun getFullStackTrace(throwable: Throwable): String {
    val out = StringWriter()
    throwable.printStackTrace(PrintWriter(out))
    return out.toString()
  }
  
  fun getTemplater(values: Map<String, Any>, prefix: String = "\${", suffix: String = "}"): StringSubstitutor {
    return StringSubstitutor(values, prefix, suffix)
  }
  
  /**
   * Format is specified with ::
   * Example "${key::-03d}"
   * Value after :: is the part also in String.format() after % char
   */
  fun getTemplaterWithFormatterSupport(values: Map<String, Any>, prefix: String = "\${", suffix: String = "}"): StringSubstitutor {
    val lookup = FormattedStringLookup(StringLookupFactory.INSTANCE.mapStringLookup<Any>(values))
    
    return StringSubstitutor(lookup, prefix, suffix, StringSubstitutor.DEFAULT_ESCAPE)
  }
  
  fun fillTemplate(template: String, values: Map<String, Any>): String {
    return getTemplater(values).replace(template)
  }
  
  fun generateId(size: Int): String {
    val bytes = ByteArray(ceil(size * 0.75).toInt())
    ThreadLocalRandom.current().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
  }
  
  fun bytesToHex(bytes: ByteArray): String {
    val hexChars = ByteArray(bytes.size * 2)
    for (j in bytes.indices) {
      val v = bytes[j].toInt() and 0xFF
      hexChars[j * 2] = HEX_ARRAY[v ushr 4]
      hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
    }
    return String(hexChars, StandardCharsets.UTF_8)
  }
  
  fun buildInHash(inputStream: InputStream, algorithm: String = "MD5", bufferSize: Int = 4096): String {
    val md: MessageDigest = MessageDigest.getInstance(algorithm)
    
    val result = BufferedInputStream(inputStream).use { bis ->
      val buf = ByteArray(bufferSize)
      var read: Int
      read = bis.read(buf)
      
      while (read != -1) {
        md.update(buf, 0, read)
        read = bis.read(buf)
      }
      md.digest()
    }
    return bytesToHex(result)
  }
  
  private class FormattedStringLookup(private val base: StringLookup) : StringLookup {
    @Deprecated("Deprecated in Java")
    override fun lookup(key: String): String? {
      if (key.contains("::")) {
        SUPPORTED_FORMATTER_CONVERSIONS
        
        try {
          val (k, conversion) = key.split("::")
          
          val specifier = conversion.last()
          if (specifier !in SUPPORTED_FORMATTER_CONVERSIONS) {
            throw IncorrectConverstionException("Conversion specifier '$specifier' is not supported.", conversion, specifier)
          }
          
          val value = base.apply(k)
          if (value == null) {
            throw KeyNotFoundException("Key '$k' was not found in values map.", k)
          }
          
          val converted = when (specifier) {
            'f' -> value.toDouble()
            's', 'b' -> value
            else -> value.toLong()
          }
          
          return String.format("%$conversion", converted)
        } catch (e: IllegalFormatException) {
          log.error("Cannot format value for key '{}': {}: {}", key, e.javaClass.simpleName, e.message)
          return key
        }
      }
      return base.apply(key)
    }
  }
  
  class KeyNotFoundException(message: String, val key: String) : RuntimeException(message)
  class IncorrectConverstionException(message: String, val conversion: String, val specifier: Char) : RuntimeException(message)
}
