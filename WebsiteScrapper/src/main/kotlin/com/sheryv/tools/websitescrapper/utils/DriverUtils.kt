package com.sheryv.tools.websitescrapper.utils

import com.sheryv.tools.websitescrapper.SystemType
import com.sheryv.tools.websitescrapper.SystemUtils
import com.sheryv.tools.websitescrapper.browser.BrowserDef
import com.sheryv.tools.websitescrapper.browser.BrowserType
import com.sheryv.tools.websitescrapper.process.base.model.SeleniumDriver
import java.io.File
import java.io.FilenameFilter

object DriverUtils {
  const val DEFAULT_SCRIPT_NAME = "common.js"
  
  fun loadScriptFromClassPath(scriptName: String): String {
    val common = DriverUtils::class.java.classLoader.getResourceAsStream(DEFAULT_SCRIPT_NAME)?.use {
      it.readAllBytes()
    }?.let { String(it) }
    val script = String(DriverUtils::class.java.classLoader.getResourceAsStream("scripts/$scriptName.js")?.use {
      it.readAllBytes()
    } ?: throw IllegalArgumentException("Script '$scriptName.js' could not be found in classpath"))
    return "$common;\n$script"
  }
  
  fun findUserDataPathForBrowser(type: BrowserType): String? {
    val path = when (SystemUtils.currentSystem()) {
      SystemType.WINDOWS ->
        if (type == BrowserType.FIREFOX)
          File(System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles").listFiles { _, name -> name.contains(".default") }
            ?.firstOrNull()
        else
          File(
            System.getenv("LOCALAPPDATA") + "\\" + when (type) {
              BrowserType.BRAVE -> "BraveSoftware\\Brave-Browser\\User Data"
              BrowserType.EDGE -> "Microsoft\\Edge\\User Data"
              BrowserType.CHROME -> "Google\\Chrome\\User Data"
              else -> return null
            }
          )
      
      SystemType.LINUX ->
        if (type == BrowserType.FIREFOX)
          File("~/.mozilla/firefox").listFiles { _, name -> name.contains(".default") }?.firstOrNull()
        else
          File(
            "~/.config/" + when (type) {
              BrowserType.BRAVE -> "brave"
              BrowserType.EDGE -> "microsoft-edge"
              BrowserType.CHROME -> "google-chrome"
              else -> return null
            }
          )
      
      SystemType.MAC ->
        if (type == BrowserType.FIREFOX)
          File("~/Library/Application Support/Firefox/Profiles").listFiles { _, name -> name.contains(".default") }
            ?.firstOrNull()
        else
          File(
            "~/Library/Application Support/" + when (type) {
              BrowserType.BRAVE -> "BraveSoftware/Brave-Browser"
              BrowserType.EDGE -> "Microsoft/Edge"
              BrowserType.CHROME -> "Google/Chrome"
              else -> return null
            }
          )
    }
    
    return path?.takeIf { it.exists() && it.isDirectory }?.absolutePath
  }
}
