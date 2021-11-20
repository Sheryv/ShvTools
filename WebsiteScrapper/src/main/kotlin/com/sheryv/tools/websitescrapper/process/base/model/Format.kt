package com.sheryv.tools.websitescrapper.process.base.model

enum class Format(val extension: String) {
  CSV("csv"),
  JSON("json"),
  TEXT("txt")
  
  ;
  
  fun toGlob(): String {
    return "*.$extension"
  }
}
