package com.sheryv.util.fx.core.app

abstract class AppConfiguration {
  abstract val name: String
  abstract val iconPath: String
  
  companion object {
    fun empty() = object : AppConfiguration() {
      override val name: String = "ShvTools App"
      override val iconPath: String = ""
    }
  }
}
