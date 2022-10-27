package com.sheryv.tools.websitescraper.process.base.model

enum class Format(val extension: String) {
  CSV("csv"),
  JSON("json"),
  TEXT("txt")
  
  ;
  
  fun toGlob(): String {
    return "*.$extension"
  }
}
