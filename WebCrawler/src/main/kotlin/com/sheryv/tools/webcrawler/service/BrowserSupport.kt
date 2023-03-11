package com.sheryv.tools.webcrawler.service

import com.sheryv.tools.webcrawler.browser.BrowserConfig
import com.sheryv.tools.webcrawler.browser.BrowserTypes
import com.sheryv.tools.webcrawler.config.Configuration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

private const val DEFAULT_SCRIPT_NAME = "common.js"

abstract class BrowserSupport {
  
  fun buildPossibleBrowserPaths(type: BrowserTypes): List<String> {
    val prefix = type.pathParts.prefixes[SystemSupport.currentSystem]
    return prefix?.let { buildPossibleBrowserPathsWithPrefix(it) } ?: emptyList()
  }
  
  fun findBrowserPath(type: BrowserTypes): Path? {
    return buildPossibleBrowserPaths(type)
      .map { Path.of(it) }
      .firstOrNull {
        it.exists() && it.isRegularFile()
      }
      ?: if (SystemSupport.currentSystem == SystemType.WINDOWS && type.pathParts.registryKey() != null) {
        val p = SystemSupport.readWindowsRegistry(type.pathParts.registryKey()!!)
        if (p != null && Path.of(p).exists()) {
          return Path.of(p)
        } else {
          null
        }
      } else {
        null
      }
  }
  
  fun loadScriptFromClassPath(scriptName: String): String {
//    val common = javaClass::class.java.classLoader.getResourceAsStream(DEFAULT_SCRIPT_NAME)?.use {
//      it.readAllBytes()
//    }?.let { String(it) } ?: ""
    val script = String(javaClass.getResourceAsStream("/scripts/$scriptName.js")?.use {
      it.readAllBytes()
    } ?: throw IllegalArgumentException("Script '$scriptName.js' could not be found in classpath"))
//    return "$common;\n$script"
    return script
  }
  
  abstract fun getPathForUserProfileInBrowser(browser: BrowserConfig, type: BrowserTypes): Path?
  
  protected abstract fun buildPossibleBrowserPathsWithPrefix(prefix: String): List<String>
  
  companion object {
    @JvmStatic
    val get by lazy {
      when (SystemSupport.currentSystem) {
        SystemType.WINDOWS -> WindowsBrowserSupport()
        SystemType.MAC -> MacOsBrowserSupport()
        SystemType.LINUX -> LinuxBrowserSupport()
      }
    }
  }
}

private class WindowsBrowserSupport : BrowserSupport() {
  override fun buildPossibleBrowserPathsWithPrefix(prefix: String) = listOf(
    "%localappdata%\\$prefix",
    "%programfiles%\\$prefix",
    "%programfiles(x86)%\\$prefix"
  )
  
  override fun getPathForUserProfileInBrowser(browser: BrowserConfig, type: BrowserTypes): Path? {
    val firefoxUserProfilePattern = Configuration.property("firefox.browser.profile.name-pattern")!!
    return (if (type == BrowserTypes.FIREFOX)
      Files.newDirectoryStream(Paths.get(System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles"), firefoxUserProfilePattern)
        .firstOrNull()
    else
      Path.of(
        System.getenv("LOCALAPPDATA") + "\\" + when (type) {
          BrowserTypes.BRAVE -> "BraveSoftware\\Brave-Browser\\User Data"
          BrowserTypes.EDGE -> "Microsoft\\Edge\\User Data"
          BrowserTypes.CHROME -> "Google\\Chrome\\User Data"
          else -> when(browser.binaryPath!!.fileName.toString().lowercase()) {
            "brave.exe" -> "BraveSoftware\\Brave-Browser\\User Data"
            "msedge.exe" -> "Microsoft\\Edge\\User Data"
            "chrome.exe" -> "Google\\Chrome\\User Data"
            else -> null
          }
        }
      ))?.takeIf { it.exists() && it.isDirectory() }
  }
}

private abstract class UnixBrowserSupport : BrowserSupport()

private class MacOsBrowserSupport : UnixBrowserSupport() {
  override fun buildPossibleBrowserPathsWithPrefix(prefix: String) = listOf(
    "/Applications/$prefix.app"
  )
  
  override fun getPathForUserProfileInBrowser(browser: BrowserConfig, type: BrowserTypes): Path? {
    val firefoxUserProfilePattern = Configuration.property("firefox.browser.profile.name-pattern")!!
    return (if (type == BrowserTypes.FIREFOX)
      Files.newDirectoryStream(Paths.get("~/Library/Application Support/Firefox/Profiles"), firefoxUserProfilePattern)
        .firstOrNull()
    else
      Path.of(
        "~/Library/Application Support/" + when (type) {
          BrowserTypes.BRAVE -> "BraveSoftware/Brave-Browser"
          BrowserTypes.EDGE -> "Microsoft/Edge"
          BrowserTypes.CHROME -> "Google/Chrome"
          else -> return null
        }
      ))?.takeIf { it.exists() && it.isDirectory() }
  }
}

private class LinuxBrowserSupport : UnixBrowserSupport() {
  override fun buildPossibleBrowserPathsWithPrefix(prefix: String) = listOf(
    "/usr/bin/$prefix"
  )
  
  override fun getPathForUserProfileInBrowser(browser: BrowserConfig, type: BrowserTypes): Path? {
    val firefoxUserProfilePattern = Configuration.property("firefox.browser.profile.name-pattern")!!
    return (if (type == BrowserTypes.FIREFOX)
      Files.newDirectoryStream(Paths.get("~/.mozilla/firefox"), firefoxUserProfilePattern)
        .firstOrNull()
    else
      Path.of(
        "~/.config/" + when (type) {
          BrowserTypes.BRAVE -> "brave"
          BrowserTypes.EDGE -> "microsoft-edge"
          BrowserTypes.CHROME -> "google-chrome"
          else -> return null
        }
      ))?.takeIf { it.exists() && it.isDirectory() }
  }
}
