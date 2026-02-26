package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model.VideoUrl
import com.sheryv.util.inBackground
import com.sheryv.util.io.DataTransferProgress
import com.sheryv.util.logging.log
import com.sheryv.util.unit.BinaryTransferSpeed
import java.nio.file.Path

class YtDlpDownloadingTask(
  output: Path,
  url: VideoUrl,
  config: DownloaderConfig
) : DownloadingTask(output, url, config = config) {
  private val m3u8Options = listOf("--hls-use-mpegts")
  private val baseOptions = listOf(
    "--enable-file-urls",
    "--paths",
    "infojson:.metadata",
    "--paths",
    output.parent.toAbsolutePath().toString().replace('\\', '/'),
//    "--paths temp:\"\${temp_dir}\"",
    "--progress-template",
    "%(progress.downloaded_bytes)d:%(progress.total_bytes)d:%(progress.total_bytes_estimate)d:%(progress.eta)d:%(progress.speed)d:%(progress.fragment_count)d",
    "--concurrent-fragments",
    config.connectionsPerFile.toString(),
    "-q",
    "--progress",
    "--cache-dir",
    config.tempDirPath.resolve("yt-dlp_cache").toAbsolutePath().toString().replace('\\', '/'),
    "-o",
    output.fileName.toString(),
    "--newline",
    "--write-info-json",
    "--progress-delta",
    "0.25",
    "--no-clean-info-json"
  )
  
  private val program = "yt-dlp"
  private lateinit var process: Process
  private lateinit var progress: DataTransferProgress
  private var partsNumber: Int = 0

//  private val tempDir: Path = config.tempDirPath.resolve(id).toAbsolutePath()
  
  override suspend fun preProcess() {
//    var headers = url.metadata.headers.flatMap { (k, v) -> listOf("--add-headers", "$k:$v") }
    val options = mutableListOf(program)
    if (url.isStreaming) {
      options.addAll(m3u8Options)
    }
    options.addAll(baseOptions)
//    options.addAll(headers)
    browserProfilePath()?.also {
      options.add("--cookies-from-browser")
      options.add("chrome:$it")
    }
    options.add("\"${url}\"")
    
    log.debug("Running yt-dlp: ${options.joinToString(" ")}")
    process = ProcessBuilder(options).start()
    
    val errorReader = process.errorStream.bufferedReader()
    inBackground {
      var line = ""
      while (errorReader.readLine()?.also { line = it } != null) {
        log.error("Error in yt-dlp: {}", line)
      }
    }
    val inputReader = process.inputReader()
    inBackground {
      var lastPrint = System.currentTimeMillis()
      var line = ""
      while (inputReader.readLine()?.also { line = it } != null) {
        if (line.contains(":")) {
//          7132720:7132720:NA:NA:5505395:12
          val parts = line.split(':')
          progress = DataTransferProgress(
            initTime!!,
            parts[1].toLongOrNull() ?: parts[2].toLongOrNull(),
            parts[4].toDoubleOrNull() ?: 0.0,
            parts[0].toLongOrNull() ?: 0
          )
          partsNumber = parts[5].toIntOrNull() ?: 0
          if (System.currentTimeMillis() - lastPrint > 1000){
            lastPrint = System.currentTimeMillis()
            
            log.debug("[D] {}: {}", output.fileName, progress)
          }
          log.trace("progress: {}", progress)
        } else {
          log.debug("yt-dlp out: {}", line)
        }
      }
    }
  }
  
  override suspend fun transfer() {
    val code = process.waitFor()
    if (code != 0) {
      changeState(DownloadingState.FAILED)
    }
  }
  
  
  override suspend fun postProcess(): Path {
//    if (tempDir.exists() && Files.list(tempDir).count() == 0L) {
//      Files.delete(tempDir)
//    }
    return output
  }
  
  override fun avgSpeed(durationMillis: Long): BinaryTransferSpeed {
    return progress.avgSpeed
  }
  
  override fun stop() {
    
    if (process.isAlive) {
      process.destroy()
    }
  }
  
  override fun extrapolateSize(): Long {
    return progress.totalSizeBytes ?: 0
  }
  
  override fun currentlyDownloadedBytes(): Long {
    return progress.currentSizeBytes
  }
  
  override fun progress(): DataTransferProgress? {
    if (!state.hasStats()) {
      return null
    }
    
    return progress
  }
  
  fun partsNumber() = partsNumber
  
  private fun browserProfilePath(): Path? {
    val browser = Configuration.get().browserSettings.currentBrowser()
    return browser.userProfilePath ?: browser.type.getPathForUserProfileInBrowser(browser)
  }
}
