package com.sheryv.tools.filematcher.model

class ValidationError : RuntimeException {
  
  val result: ValidationResult
  
  constructor(result: ValidationResult, ex: Exception? = null) : super("Problems: \n" + result.toLongText(), ex) {
    this.result = result
    check(!result.isOk())
  }
  
  constructor(text: String, ex: Exception? = null) : this(ValidationResult(text), ex)
  
  override fun toString(): String {
    return message ?: "ValidationError"
  }
}
