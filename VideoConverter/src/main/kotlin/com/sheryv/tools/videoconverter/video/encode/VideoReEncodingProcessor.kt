package com.sheryv.tools.videoconverter.video.encode

import com.sheryv.tools.videoconverter.core.Context
import com.sheryv.tools.videoconverter.video.convert.ConversionProcessState
import com.sheryv.util.io.FileUtils
import com.sheryv.util.logging.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.AutoCloseable
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class VideoReEncodingProcessor(
  private val context: Context,
  private val onProgress: (String) -> Unit,
  private val onComplete: (Exception?) -> Unit
) : AutoCloseable {
  
  private val settings = context.settings.videoEncodingSettings
  
  private val processes: Queue<Process> = ConcurrentLinkedQueue()
  private val threadPool = Executors.newFixedThreadPool(context.settings.parallelProcessing)
  
  suspend fun encode(records: List<VideoToEncodeFile>) {
    try {
      if (!settings.cliPath.exists()){
        throw RuntimeException("CLI executable pointed by '${settings.cliPath}' does not exist")
      }
      
      withContext(context.mainDispatcher) {
        onProgress("Processing videos...")
      }
      run(records)
      
      withContext(context.mainDispatcher) {
        onProgress("Completed")
        onComplete(null)
      }
    } catch (e: Exception) {
      log.error("Error in process", e)
      withContext(context.mainDispatcher) {
        onProgress("Completed")
        onComplete(e)
      }
    }
  }
  
  fun search(): List<VideoToEncodeFile> {
    val matcher = FileSystems.getDefault().getPathMatcher("glob:${settings.globPathFilter}")
    return Files.walk(settings.baseSearchPath, 30)
      .filter { Files.isRegularFile(it) }
      .filter { matcher.matches(it) }
      .map {
        val relative = settings.baseSearchPath.relativize(it)
        var out = if (settings.outputPath.isAbsolute) {
          settings.outputPath.resolve(relative)
        } else {
          settings.baseSearchPath.resolve(settings.outputPath).resolve(relative)
        }
        
        out = FileUtils.replaceExtension(out, "mkv")
        
        VideoToEncodeFile(relative, settings.defaultQuality, ConversionProcessState.READY, out)
      }
      .toList()
  }
  
  private suspend fun run(records: List<VideoToEncodeFile>) {
    val futures = records.filter { it.state == ConversionProcessState.READY }.mapNotNull { video ->
      try {
        Files.createDirectories(video.outputPath.parent)
        
        if (Files.exists(video.outputPath)) {
          FileUtils.renameAsBackup(video.outputPath)
        }
        
        withContext(context.mainDispatcher) {
          video.stateProperty.value = ConversionProcessState.IN_QUEUE
        }
        
        threadPool.submit(Callable {
          runBlocking {
            withContext(context.mainDispatcher) {
              video.stateProperty.value = ConversionProcessState.PROCESSING
            }
            
            runSingle(video)
          }
        }) to video
      } catch (e: Exception) {
        log.error(e.message, e)
        null
      }
    }
    
    for ((future, video) in futures) {
      try {
        val success = future.get()
        
        log.info("Done {}", video.path)
        
        val state = if (success && Files.exists(video.outputPath)) {
          ConversionProcessState.COMPLETED
        } else {
          ConversionProcessState.FAILED
        }
        
        withContext(context.mainDispatcher) {
          video.stateProperty.value = state
        }
        
      } catch (e: Exception) {
        log.error(e.message, e)
        
        withContext(context.mainDispatcher) {
          video.stateProperty.value = ConversionProcessState.FAILED
        }
      }
    }
    
  }
  
  fun runSingle(video: VideoToEncodeFile): Boolean {
    var process: Process? = null
    try {
      val cmd = commandFor(video)
      val workDir = settings.baseSearchPath
      
      log.debug("RUN: {}", cmd.joinToString(" "))
      
      workDir.resolve(video.path).parent.listDirectoryEntries("*.nfo").forEach {
        it.copyTo(video.outputPath.parent.resolve(it.name), true)
      }
      val srt = workDir.resolve(video.path).parent.resolve(video.path.nameWithoutExtension + ".srt")
      if (srt.exists()) {
        srt.copyTo(video.outputPath.parent.resolve(srt.name), true)
      }
      
      val builder = ProcessBuilder(cmd)
        .directory(workDir.toFile())
        .redirectErrorStream(true)
      
      process = builder.start()
      processes.add(process)
      
      val progressRegex = Regex("""(\d+(?:\.\d+)?)\s*%""")
      
      process.inputStream.bufferedReader().use { reader ->
        val sb = StringBuilder()
        var c: Int
        
        while (reader.read().also { c = it } != -1) {
          val char = c.toChar()
          if (char == '\r' || char == '\n') {
            val line = sb.toString()
            sb.clear()
            
            if (line.isNotBlank()) {
              val match = progressRegex.find(line)
              if (match != null) {
                val percentageStr = match.groupValues[1]
                percentageStr.toDoubleOrNull()?.let { progressValue ->
                    video.progressProperty.value = progressValue
                }
              } else if (line.contains("error", ignoreCase = true)) {
                log.error("Handbrake Error: {}", line)
              }
            }
          } else {
            sb.append(char)
          }
        }
      }
      
      val exitCode = process.waitFor()
      if (exitCode == 0) {
          video.progressProperty.value = 100.0
        }
      return exitCode == 0
    } finally {
      if (process != null) {
        processes.remove(process)
      }
    }
  }
  
  fun commandFor(video: VideoToEncodeFile): List<String> {
    val path = settings.baseSearchPath.resolve(video.path).absolutePathString()
    
    return listOf(
      settings.cliPath.toString(),
      "-i", path,
      "-o", video.outputPath.absolutePathString(),
      "-q", video.qualityProperty.value.toString(),
      "-e", "nvenc_h265",
      "--encoder-preset", "slow",
      "-f", "av_mkv",
      "--align-av",
      "--no-markers",
      "--all-audio",
      "--all-subtitles",
      "-E", "copy",
    )
  }
  
  override fun close() {
    threadPool.shutdownNow()
    
    for (process in processes.toList()) {
      try {
        process.destroy()
      } catch (e: Exception) {
        log.error("Cannot kill process", e)
      }
    }
  }
  
}
