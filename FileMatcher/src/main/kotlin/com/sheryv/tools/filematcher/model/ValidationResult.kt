package com.sheryv.tools.filematcher.model

class ValidationResult(val errors: MutableList<String> = mutableListOf()) {
  fun isOk(): Boolean {
    return errors.isEmpty()
  }
  
  fun merge(result: ValidationResult): ValidationResult {
    errors.addAll(result.errors)
    return this
  }
  
  fun assert(cond: Boolean?, error: String): ValidationResult {
    if (cond == false) {
      errors.add(error)
    }
    return this
  }
  
  fun addError(error: String): ValidationResult {
    errors.add(error)
    return this
  }
  
  constructor(vararg errors: String) : this(mutableListOf(*errors))
}