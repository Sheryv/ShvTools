package com.sheryv.tools.videoconverter

import com.sheryv.tools.videoconverter.video.MainSettings
import com.sheryv.tools.videoconverter.video.process.Language
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class Context(val settings: MainSettings) {
  val languages by lazy { loadLanguages() }
  
  var mainDispatcher: CoroutineDispatcher = Dispatchers.Main
  
  constructor(settings: MainSettings, mainDispatcher: CoroutineDispatcher): this(settings) {
    this.mainDispatcher = mainDispatcher
  }
  
  fun isLanguageSupported(language: String?, includeUndefined: Boolean = true): Boolean {
    if (includeUndefined && (language == null || language == "und")) {
      return true
    }
    if (!includeUndefined && (language == null || language == "und")) {
      return false
    }
    
    val lang = findLanguage(language!!)
    return lang != null && (
        settings.languageFilter.isEmpty()
            || settings.languageFilter.contains(lang.code)
            || settings.languageFilter.contains(lang.longCode))
  }
  
  fun findLanguage(language: String): Language? {
    return languages.firstOrNull { it.code == language.lowercase() || it.longCode == language.lowercase() }
  }
  
  private fun loadLanguages(): List<Language> {
    return javaClass.classLoader.getResourceAsStream("languages.csv")
      .use { it.bufferedReader().readLines() }
      .map { it.split(';').map { it.trim() } }
      .map { Language(it[0], it[1], it[2], it[3]) }
  }
}
