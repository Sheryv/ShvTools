package com.sheryv.tools.filematcher.model

import java.lang.RuntimeException

data class ValidationError(val result: ValidationResult) : RuntimeException("Problems: \n" + result.toLongText()) {
  
  init {
    check(!result.isOk())
  }
  
}