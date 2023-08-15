package com.sheryv.tools.webcrawler.service

import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.util.logging.log
import java.awt.Desktop
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.listDirectoryEntries

enum class SystemType {
  WINDOWS,
  MAC,
  LINUX
}

abstract class SystemSupport {
  
  val userDir by lazy {
    Path.of(System.getProperty("user.home"))
  }
  
  val PROPERTIES: Map<String, Any> by lazy {
    val map = System.getProperties().mapKeys { it.key.toString() }.toMutableMap()
    map.putAll(System.getenv())
    map.mapKeys { it.key.lowercase() }
  }
  
  val userDownloadDir by lazy {
    Path.of(System.getProperty("user.home"), "Downloads").takeIf { it.exists() } ?: userDir
  }
  
  fun isWindowsOS(): Boolean = currentSystem == SystemType.WINDOWS
  
  fun encodeNameForWeb(text: String): String {
    return URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")
  }
  
  fun removeForbiddenFileChars(text: String, replaceChar: Char = '_'): String {
    var name = text
    for (c in fileNameForbiddenChars) {
      name = name.replace(c, replaceChar)
    }
    return name
  }
  
  fun parseDirectory(initialDirectory: Path?, default: Path? = userDownloadDir): Path? {
    return initialDirectory?.let { f ->
      if (f.exists()) {
        val res = if (f.isDirectory()) {
          f
        } else {
          f.parent
        }
        log.debug("Directory parse result: [$f] -> [$res]")
        res
      } else {
        log.info("Directory parse with null result: [$f]")
        null
      }
    } ?: default
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
      openLinkInternal(webpage)
    }
  }
  
  fun runApp(appCmd: String) {
    Runtime.getRuntime().exec(appCmd)
  }
  
  fun runAppWithResult(appCmd: String): String {
    val p = Runtime.getRuntime().exec(appCmd)
    p.waitFor()
    return String(p.inputStream.readAllBytes())
  }
  
  fun findExecutablePath(name: String): Path? {
    val found = System.getenv("PATH").split(File.pathSeparatorChar).flatMap {
      Path.of(it).listDirectoryEntries("$name*").filter { it.isExecutable() }
    }
    if (found.size > 1) {
      log.warn("Found many executables on path for name '$name'\n", found.joinToString("\n"))
    }
    return found.firstOrNull()
  }
  
  abstract val fileNameForbiddenChars: Array<Char>
  
  abstract fun runCmd(cmd: String)
  
  abstract fun runCmdWithResult(cmd: String): String
  
  abstract fun getFileVersion(path: Path): List<String>
  
  protected abstract fun openLinkInternal(webpage: String)
  
  companion object {
    val currentSystem by lazy {
      val s = System.getProperty("os.name").lowercase()
      when {
        s.contains("windows") -> SystemType.WINDOWS
        s.startsWith("mac os") -> SystemType.MAC
        else -> SystemType.LINUX
      }
    }
    
    @JvmStatic
    val get by lazy {
      when (currentSystem) {
        SystemType.WINDOWS -> WindowsSystemSupport()
        SystemType.MAC -> MacOsSystemSupport()
        SystemType.LINUX -> LinuxSystemSupport()
      }
    }
    
    @JvmStatic
    fun readWindowsRegistry(key: String): String? {
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
        log.error("Cannot read registry ", e)
        return null
      }
    }
  }
}

private class WindowsSystemSupport : SystemSupport() {
  override val fileNameForbiddenChars = arrayOf('<', '>', ':', '"', '/', '\\', '|', '?', '*').let {
    val list = (0..31).map { it.toChar() }.toMutableList()
    list.addAll(it)
    list.remove(' ')
    list.toTypedArray()
  }
  
  override fun openLinkInternal(webpage: String) {
    runCmd("start $webpage")
  }
  
  override fun runCmd(cmd: String) = runApp("cmd /k '$cmd'")
  
  override fun runCmdWithResult(cmd: String) = runAppWithResult("cmd /k '$cmd'")
  
  override fun getFileVersion(path: Path): List<String> {
    return runAppWithResult(
      "wmic datafile where name='${path.toAbsolutePath().toString().replace("""\""", """\\""")}' get version"
    )
      .lines()
      .map { it.trim() }
      .first { it.isNotEmpty() && it[0].isDigit() }
      .split('.')
      .map { it.trim() }
  }
}

private abstract class UnixSystemSupport : SystemSupport() {
  private val versionRegex = Regex("""\d+.\d+(.\d+.\d+)?""")
  
  override val fileNameForbiddenChars = arrayOf('/', 0.toChar())
  
  override fun openLinkInternal(webpage: String) {
    val browsers = arrayOf(
      "chrome", "chromium", "epiphany", "firefox", "mozilla", "konqueror",
      "netscape", "opera", "links", "lynx"
    )
    
    val cmd = StringBuffer()
    for (i in browsers.indices) {
      if (i == 0) {
        cmd.append(String.format("%s \"%s\"", browsers[i], webpage))
      } else {
        cmd.append(String.format(" || %s \"%s\"", browsers[i], webpage))
      }
    }
    Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd.toString()))
  }
  
  override fun runCmd(cmd: String) = runApp("sh -c '$cmd'")
  
  override fun runCmdWithResult(cmd: String) = runAppWithResult("sh -c '$cmd'")
  
  override fun getFileVersion(path: Path): List<String> {
    return if (path.exists()) {
      val ver = runAppWithResult(path.toAbsolutePath().toString() + " --version")
      versionRegex.find(ver)?.value?.split('.') ?: emptyList()
    } else {
      emptyList()
    }
  }
}

private class LinuxSystemSupport : UnixSystemSupport() {

}

private class MacOsSystemSupport : UnixSystemSupport() {

}
