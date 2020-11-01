package com.sheryv.tools.filematcher.model

import java.lang.RuntimeException

class ValidationError(val result: ValidationResult) : RuntimeException("Error: " + result.errors.joinToString { ", \n" }) {
  
  init {
    check(!result.isOk())
  }
  
  fun toLongText(): String {
    return result.errors.mapIndexed() { i, e ->
      "${(i + 1).toString().padStart(2)}. $e\n"
    }.joinToString("\n")
  }
}