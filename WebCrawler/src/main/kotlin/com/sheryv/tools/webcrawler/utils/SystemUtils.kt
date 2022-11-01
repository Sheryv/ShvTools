package com.sheryv.tools.webcrawler


import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.util.Strings
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.io.path.isExecutable
import kotlin.io.path.listDirectoryEntries


object SystemUtils {
  private val WIN_CHARS = arrayOf('<', '>', ':', '"', '/', '\\', '|', '?', '*').let {
    val list = (0..31).map { it.toChar() }.toMutableList()
    list.addAll(it)
    list.remove(' ')
    list.toTypedArray()
  }
  private val UNIX_CHARS = arrayOf('/', 0.toChar())
  private val PROPS: Map<String, Any> by lazy {
    val map = System.getProperties().mapKeys { it.key.toString() }.toMutableMap()
    map.putAll(System.getenv())
    map.mapKeys { it.key.lowercase() }
  }
  
  fun userDir(): String {
    return System.getProperty("user.home")
  }
  
  fun userDownloadDir(): String {
    return Paths.get(System.getProperty("user.home"), "Downloads").toAbsolutePath().toString()
  }
  
  fun isWindowsOS(): Boolean = currentSystem() == SystemType.WINDOWS
  
  fun currentSystem(): SystemType {
    val s = System.getProperty("os.name").lowercase()
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
  
  fun removeForbiddenFileChars(text: String, replaceChar: Char = '_'): String {
    var name = text
    for (c in fileNameForbiddenChars()) {
      name = name.replace(c, replaceChar)
    }
    return name
  }
  
  fun encodeNameForWeb(text: String): String {
    return URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")
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
        val browsers = arrayOf(
          "epiphany", "firefox", "mozilla", "chrome", "chromium", "konqueror",
          "netscape", "opera", "links", "lynx"
        )
        
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
    return Strings.fillTemplate(s, PROPS)
  }
  
  fun getFileVersion(path: Path): List<Int> {
    return when (currentSystem()) {
      SystemType.WINDOWS -> {
        val p =
          Runtime.getRuntime().exec("wmic datafile where name='${path.toAbsolutePath().toString().replace("""\""", """\\""")}' get version")
        p.waitFor()
        String(p.inputStream.readAllBytes()).lines().map { it.trim() }.first { it.isNotEmpty() && it[0].isDigit() }.split('.')
          .mapNotNull { it.toIntOrNull() }
      }
      else -> throw UnsupportedOperationException()
    }
  }
  
  fun findExecutablePath(name: String): Path? {
    val found = System.getenv("PATH").split(File.pathSeparatorChar).flatMap {
      Path.of(it).listDirectoryEntries("$name*").filter { it.isExecutable() }
    }
    if (found.size > 1) {
      lg().warn("Found many executable location on path for name '$name'\n", found.joinToString("\n"))
    }
    return found.firstOrNull()
  }

//  fun findBrowserPathAndFill(registry: BrowserRegistry) {
//    if (isWindowsOS()) {
//      for (b in registry.all()) {
//        b.defaultPath = null
//        val path = readRegistry(b.defaultRegistryKey ?: "HKCR\\${b}HTML\\shell\\open\\command")
//        if (path != null && File(path).exists()) {
//          b.defaultPath = path
//        }
//      }
//    } else {
//      for (b in registry.all()) {
//        b.defaultPath = null
//        val pathname = (if (currentSystem() == SystemType.LINUX) b.linuxPath else b.macPath) ?: continue
//        val path = File(pathname)
//        if (path.exists()) {
//          b.defaultPath = path.absolutePath
//        }
//      }
//    }
//  }
  
  fun findBrowserPath(registryKey: String? = null, paths: List<String>): String? {
    if (isWindowsOS() && registryKey != null) {
      val p = readRegistry(registryKey)
      if (p != null && File(p).exists()) {
        return File(p).absolutePath
      }
    }
    if (paths.isNotEmpty()) {
      return paths
        .map { File(it) }
        .firstOrNull {
          it.exists() && it.isFile
        }?.absolutePath
    }
    return null
  }
  
  //  fun defaultBrowserPath(binaryName: String, vararg prefixes: String): List<String> {
//    val part = if (prefixes.isNotEmpty())
//      Path.of(prefixes.first(), *prefixes.drop(1).toTypedArray()).toString() + File.pathSeparator
//    else ""
//    return when (currentSystem()) {
//      SystemType.WINDOWS -> {
//        listOf("%localappdata%\\$part${binaryName}")
//        listOf("%programfiles%\\$part${binaryName}")
//        listOf("%programfiles(x86)%\\$part${binaryName}")
//      }
//      SystemType.LINUX -> listOf("/usr/bin/$part${binaryName}")
//      SystemType.MAC -> listOf("/Applications/$part${binaryName}.app")
//    }
//  }
//
  fun buildDefaultBrowserPaths(system: SystemType, prefix: String): List<String> {
    return when (system) {
      SystemType.WINDOWS -> {
        listOf(
          "%localappdata%\\$prefix",
          "%programfiles%\\$prefix",
          "%programfiles(x86)%\\$prefix"
        )
      }
      SystemType.LINUX -> listOf("/usr/bin/$prefix")
      SystemType.MAC -> listOf("/Applications/$prefix.app")
    }
  }
  
  fun buildUserProfilePaths(suffix: String): String {
    return when (currentSystem()) {
      SystemType.WINDOWS -> "%localappdata%\\$suffix"
      SystemType.LINUX -> "~/.config/$suffix"
      SystemType.MAC -> "/Applications/$suffix.app"
    }
  }
  
  
  private fun readRegistry(key: String): String? {
    try {
      val processBuilder = ProcessBuilder("reg", "query", "\"$key\"", "/ve")
      
      processBuilder.redirectErrorStream(true)
      
      val process = processBuilder.start()
      val outputBuilder = StringBuilder()
      
      BufferedReader(InputStreamReader(process.inputStream)).use { processOutputReader ->
        var readLine = processOutputReader.readLine()
        while (readLine != null) {
          outputBuilder.appendLine(readLine)
          readLine = processOutputReader.readLine()
        }
        process.waitFor(2, TimeUnit.SECONDS)
      }
      
      val output = outputBuilder.toString()
      if (output.isBlank() || !output.contains("REG_SZ")) {
        return null
      }
      var indexOf = output.indexOf("REG_SZ")
      
      val part = output.substring(indexOf + 6).trimStart().trimStart('"')
      indexOf = part.indexOf('"')
      return part.take(indexOf)
    } catch (e: Exception) {
      lg().error("Cannot read registry ", e)
      return null
    }
  }
}

enum class SystemType {
  WINDOWS,
  MAC,
  LINUX
}
