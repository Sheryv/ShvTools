package com.sheryv.tools.webcrawler.process.base.model

enum class Format(val extension: String) {
  CSV("csv"),
  JSON("json"),
  TEXT("txt")
  
  ;
  
  fun toGlob(): String {
    return "*.$extension"
  }
}
