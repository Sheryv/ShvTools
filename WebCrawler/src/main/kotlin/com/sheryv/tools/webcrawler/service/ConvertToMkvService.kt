package com.sheryv.tools.webcrawler.service

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.config.impl.StreamingWebsiteSettings
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.Series
import com.sheryv.util.fx.core.view.ViewFactory
import com.sheryv.util.inBackground
import com.sheryv.util.logging.log
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path

class ConvertToMkvService(
  private val config: Configuration,
  private val settings: StreamingWebsiteSettings,
  private val viewFactory: ViewFactory
) {
  
  suspend fun convert(series: Series, onProgress: suspend (Double) -> Unit): Boolean {
    if (settings.converterExePath.isNullOrBlank() || !Files.exists(Path.of(settings.converterExePath))) {
      throw IllegalArgumentException("Path to converter executable is not set or it is incorrect")
    }
    
    val toDelete = Path.of(settings.downloadDir).resolve("to_delete")
    
    val paths = series.episodes
      .map { it.generateDefaultFilePath(series, settings) }
      .map {
        val new = it.parent.resolve("_processing").resolve(it.fileName)
        Files.createDirectories(new.parent)
        Files.move(it, new)
        new
      }
    
    if (paths.isEmpty()) {
      return false
    }

//      log.debug("Convert: {}", )
    
    val dir = paths.first().parent.toAbsolutePath()
    val builder = ProcessBuilder(settings.converterExePath, "swap", "-w", "\"$dir\"", "-p", "\".*\"", "-o", "../", "-y")
      .directory(dir.toFile())
//          .inheritIO()
    
    val process = builder.start()
    
    var otherLogs: StringBuilder = StringBuilder()
    
    inBackground {
      val errors = StringWriter()
      process.errorStream.bufferedReader().use {
        it.transferTo(errors)
      }
      if (errors.buffer.isNotEmpty()) {
        log.error("Converter errors: {}", errors.buffer.toString())
      }
    }
    
    val reader = process.inputStream.bufferedReader()
    val lines = reader.lineSequence()
    
    lines.forEach {
      if (it.startsWith("Progress")) {
        val percent = it.substring("Progress: ".length, "Progress: ".length + 2).trim().toIntOrNull()
        if (percent != null) {
          log.debug("Conversion progress: {} %", percent)
          onProgress(percent / 100.0)
        }
      } else {
        otherLogs.appendLine(it)
      }
    }
    
    val exitCode = process.waitFor()
    if (otherLogs.isNotEmpty()) {
      log.debug("Converter logs: \n{}", otherLogs.toString())
    }
    if (exitCode == 0) {
      paths.forEach { Files.move(it, toDelete.resolve(it.fileName)) }
    }
    
    return exitCode == 0
  }
}

