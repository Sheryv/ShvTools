package com.sheryv.tools.websitescraper.utils

class AppError(text: String, ex: Exception? = null) : RuntimeException("Problem: \n$text", ex) {
  
  override fun toString(): String {
    return message ?: "ValidationError"
  }
}
