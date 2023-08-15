package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.util.*
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeoutException
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class M3U8DownloadingTask(
  output: Path,
  url: String,
  config: DownloaderConfig,
) : DownloadingTask(output, url, config = config) {
  
  private val tempDir = config.tempDirPath.resolve(id)
  
  private var parts: List<M3U8Part> = emptyList()
  
  private val tpe = Executors.newFixedThreadPool(config.connectionsPerFile) as ThreadPoolExecutor
  
  override suspend fun preProcess() {
    tempDir.createDirectories()
    
    val baseURL = url.substring(0, url.lastIndexOf("/") + 1)
    parts = HttpSupport().sendString(url).lines()
      .filter { it.contains(".ts") && !it.trimStart().startsWith("#") }
      .map {
        if (it.startsWith("http:") || it.startsWith("https:")) {
          it
        } else {
          baseURL + it
        }
      }
      .mapIndexed { index, url ->
        val partPath = tempDir.resolve(index.toString().padStart(5, '0') + ".ts.part")
        M3U8Part(index, partPath, FileDownloader(url, partPath))
      }
  }
  
  override suspend fun transfer() {
    tpe.setRejectedExecutionHandler { r, executor ->
    
    }
    val queue = ConcurrentLinkedQueue(parts)


//    if (Files.exists(tempDir)) {
//      Files.walk(tempDir).sorted(Comparator.reverseOrder()).use {
//        it.forEach(Files::deleteIfExists)
//      }
//    }
    
    while (!queue.isEmpty()) {
      val item = queue.poll()
      tpe.execute {
        var currentTries = 1
        while (currentTries <= config.maxRetries) {
          try {
            item.process.downloadSync()
            break
          } catch (e: Exception) {
            log.error(
              "Cannot download part ${item.index} of ${fileName()} [$id], retries left: ${config.maxRetries - currentTries}",
              e
            )
            currentTries++
            if (currentTries > config.maxRetries) {
              changeState(DownloadingState.FAILED)
            }
          }
        }
      }
    }
    tpe.shutdown()
    val waitStart = Instant.now()
    while (!tpe.isTerminated) {
      checkIfStopped()
      if (Duration.between(waitStart, Instant.now()).toHours() > 1) {
        throw TimeoutException("Timeout while waiting for thread pool")
      }
      delay(500)
    }
  }
  
  
  override suspend fun postProcess(): Path {
    
    fun getPath(base: Path): Path {
      return if (Files.exists(base)) {
        val alternative = base.parent.resolve(base.nameWithoutExtension + "_1." + base.extension)
        log.error("Output file already exists: ${fileName()} [$id] - trying as ${alternative.fileName}")
        
        getPath(alternative)
      } else {
        base
      }
    }
    
    val path = getPath(output)
    
    FileUtils.mergeFilesFromDirToSingle(parts.map { it.file }, output)
    
    checkIfStopped()
    
    if (Files.exists(tempDir)) {
      Files.walk(tempDir).sorted(Comparator.reverseOrder()).use {
        it.forEach(Files::deleteIfExists)
      }
    }
    return path
  }
  
  
  override fun avgSpeed(durationMillis: Long): FileSize {
    val started = parts.filter { it.process.started }
    return FileSize((started.mapNotNull { it.process.progress?.size }.sum() / (durationMillis / 1000.0)).toLong())
  }
  
  override fun stop() {
    if (state.isStarted) {
      changeState(DownloadingState.STOPPED)
      tpe.shutdown()
    }
  }
  
  override fun extrapolateSize(): FileSize {
    val checked = parts.filter { it.process.isComplete }.takeIf { it.isNotEmpty() }
      ?: parts.filter { it.process.started }.takeIf { it.isNotEmpty() } ?: return FileSize.ZERO
    
    val avgPartSize = (checked.mapNotNull { it.process.progress?.size }.sum() / checked.size)
    
    return FileSize(avgPartSize * parts.size)
  }
  
  override fun progress(): DownloadProgress? {
    if (!state.hasStats()) {
      return null
    }
    
    val totalSize = extrapolateSize()
    val res = DownloadProgress(totalSize.sizeInBytes)
    if (totalSize.sizeInBytes > 0) {
      val list = ArrayList(parts).filter { it.process.started }
      val bytes = list.mapNotNull { it.process.progress?.currentBytes }.sum()
      res.currentBytes = bytes
      
      val speed = (list.mapNotNull { it.process.progress?.currentSpeed }.sum() / list.size) * parts.size.coerceAtMost(config.connectionsPerFile)
      res.currentSpeed = speed
      res.currentRatio = ((bytes.toDouble() / totalSize.sizeInBytes))
    }
    return res
  }
  
  fun partsNumber() = parts.size
  
  private data class M3U8Part(val index: Int, val file: Path, val process: FileDownloader)
}
