package com.sheryv.tools.websitescraper.utils

import com.sheryv.tools.websitescraper.SystemType
import com.sheryv.tools.websitescraper.SystemUtils
import com.sheryv.tools.websitescraper.browser.BrowserType
import com.sheryv.tools.websitescraper.config.Configuration
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
    val firefoxUserProfilePattern = Configuration.property("firefox.browser.profile.name-pattern")!!
    val path = when (SystemUtils.currentSystem()) {
      SystemType.WINDOWS ->
        if (type == BrowserType.FIREFOX)
          Files.newDirectoryStream(Paths.get(System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles"), firefoxUserProfilePattern)
            .firstOrNull()
            ?.toFile()
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
          Files.newDirectoryStream(Paths.get("~/.mozilla/firefox"), firefoxUserProfilePattern)
            .firstOrNull()
            ?.toFile()
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
          Files.newDirectoryStream(Paths.get("~/Library/Application Support/Firefox/Profiles"), firefoxUserProfilePattern)
            .firstOrNull()
            ?.toFile()
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
