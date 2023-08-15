package com.sheryv.util

import org.apache.commons.text.StringSubstitutor
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.ceil

object Strings {
  private val HEX_ARRAY = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)
  fun isNullOrEmpty(string: String?): Boolean {
    return string == null || string.isEmpty()
  }
  
  fun getFullStackTrace(throwable: Throwable): String {
    val out = StringWriter()
    throwable.printStackTrace(PrintWriter(out))
    return out.toString()
  }
  
  fun getTemplater(values: Map<String, Any>): StringSubstitutor {
    return StringSubstitutor(values, "\${", "}")
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
}
