package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.SystemUtils
import com.sheryv.tools.filematcher.utils.lg

data class BasePath(
    val default: String? = null,
    val windows: String? = null,
    val unix: String? = null
) {
  
  fun findPath(): String? {
    val s = if (SystemUtils.isWindowsOS()) {
      windows ?: default
    } else {
      unix ?: default
    }
    if (!s.isNullOrBlank() && s.contains("\${")) {
      val resolved = SystemUtils.resolveEnvironmentVariables(s)
      lg().debug("Environment variables resolution: [$s] -> [$resolved]")
      return resolved
    }
    return s
  }
}