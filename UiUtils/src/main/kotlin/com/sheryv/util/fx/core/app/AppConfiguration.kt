package com.sheryv.util.fx.core.app

abstract class AppConfiguration {
  abstract val name: String
  open val theme: Theme = Theme.DARK
  
  @Transient
  open val iconPath: String = "icons/app.png"
  
  @Transient
  open val stylesPaths: AppStylePaths = AppStylePaths()
  
  companion object {
    fun empty() = object : AppConfiguration() {
      override val name: String = "ShvTools App"
    }
    
    data class AppStylePaths(
      val common: List<String> = listOf("styles/default-common-style.css"),
      val light: List<String> = listOf(),
      val dark: List<String> = listOf("styles/default-dark.css")
    )
  }
  
  enum class Theme {
    LIGHT, DARK
  }
}
