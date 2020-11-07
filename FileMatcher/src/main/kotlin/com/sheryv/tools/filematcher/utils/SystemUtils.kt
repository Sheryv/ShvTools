package com.sheryv.tools.filematcher.utils


import com.sheryv.util.Strings
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.Paths


object SystemUtils {
  private val WIN_CHARS = arrayOf('<', '>', ':', '"', '/', '\\', '|', '?', '*').let {
    val list = (0..31).map { it.toChar() }.toMutableList()
    list.addAll(it)
    list.toTypedArray()
  }
  private val UNIX_CHARS = arrayOf('/', 0.toChar())
  
  fun userDir(): String {
    return System.getProperty("user.home")
  }
  
  fun userDownloadDir(): String {
    return Paths.get(System.getProperty("user.home"), "Downloads").toAbsolutePath().toString()
  }
  
  fun isWindowsOS(): Boolean = System.getProperty("os.name").toLowerCase().contains("windows")
  
  fun currentSystem(): SystemType {
    val s = System.getProperty("os.name").toLowerCase()
    return when {
      s.contains("windows") -> SystemType.WINDOWS
      s.startsWith("mac os") -> SystemType.MAC
      else -> SystemType.LINUX
    }
  }
  
  fun fileNameForbiddenChars(): Array<Char> {
    return if (isWindowsOS()) {
      WIN_CHARS
    } else {
      UNIX_CHARS
    }
  }
  
  fun parseDirectory(initialDirectory: String?, default: String? = userDownloadDir()): File? {
    return initialDirectory?.let {
      val f = File(it)
      if (f.exists()) {
        val res = if (f.isDirectory) {
          f
        } else {
          f.parent?.let { File(it) }
        }
        lg().debug("Directory parse result: [$it] -> [$res]")
        res
      } else {
        lg().info("Directory parse with null result: [$it]")
        null
      }
    } ?: default?.let { File(it) }
  }
  
  fun openLink(webpage: String) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      val desktop = Desktop.getDesktop()
      try {
        desktop.browse(URI(webpage))
      } catch (e: IOException) {
        e.printStackTrace()
      } catch (e: URISyntaxException) {
        e.printStackTrace()
      }
    } else {
      if (isWindowsOS()) {
        Runtime.getRuntime().exec("cmd /k start $webpage")
      } else {
        val browsers = arrayOf("epiphany", "firefox", "mozilla", "konqueror",
            "netscape", "opera", "links", "lynx")
        
        val cmd = StringBuffer()
        for (i in browsers.indices) {
          if (i == 0) {
            cmd.append(java.lang.String.format("%s \"%s\"", browsers[i], webpage))
          } else {
            cmd.append(java.lang.String.format(" || %s \"%s\"", browsers[i], webpage))
          }
        }
        Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd.toString()))
      }
    }
  }
  
  fun resolveEnvironmentVariables(s: String): String {
    val map = System.getenv() as Map<String, Any>
    return Strings.fillTemplate(s, map)
  }
}

enum class SystemType {
  WINDOWS,
  MAC,
  LINUX
}