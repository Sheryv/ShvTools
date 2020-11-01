package com.sheryv.tools.filematcher.model

import com.sheryv.tools.filematcher.utils.SystemUtils

class BasePath(
    val default: String? = null,
    val windows: String? = null,
    val unix: String? = null
) {
  
  fun findPath(): String? {
    return if (SystemUtils.isWindowsOS()) {
      windows ?: default
    } else {
      unix ?: default
    }
  }
}