package com.sheryv.util

import org.apache.commons.lang3.StringUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant

object VersionUtils {
  fun loadVersionByModuleName(moduleName: String): Version {
    val resourceAsStream = VersionUtils::class.java.getClassLoader().getResourceAsStream("$moduleName.txt")
    var ms: Long = 0
    try {
      BufferedReader(InputStreamReader(resourceAsStream)).use { reader ->
        val version = reader.readLine()
        val time = reader.readLine()
        if (StringUtils.isNotBlank(time)) {
          ms = time.toLong()
        }
        return Version(version, ms)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return Version("", ms)
  }
}

class Version(val version: String, val buildTimeMs: Long) {
  
  fun toTimestamp(): Instant {
    return Instant.ofEpochMilli(buildTimeMs)
  }
  
  override fun toString(): String {
    return version + " (Built at " + toTimestamp() + ')'
  }
}
