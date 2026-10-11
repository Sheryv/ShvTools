package com.sheryv.tools.videoconverter.video.convert

import com.sheryv.tools.videoconverter.core.Context
import com.sheryv.tools.videoconverter.core.MainSettings
import com.sheryv.tools.videoconverter.video.convert.ffmpeg.FFmpegService
import com.sheryv.tools.videoconverter.video.convert.ffprobe.FFProbeResult
import com.sheryv.tools.videoconverter.video.convert.mkvmerge.MkvMergeService
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.Strings
import com.sheryv.util.io.FileUtils
import com.sheryv.util.logging.log
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.io.path.copyTo
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence

class ConversionProcessor(
  private val context: Context,
  private val onProgress: (String) -> Unit,
  private val onComplete: (Exception?) -> Unit
) : AutoCloseable {
  private val settings = context.settings
  private val filesystemOffestAttributeRegex = Regex("""offset=(-?\d+(.\d+)?)""")
  
  private val processes: Queue<Process> = ConcurrentLinkedQueue()
  private val threadPool = Executors.newFixedThreadPool(settings.parallelProcessing)
  private val ffmpeg = FFmpegService(context)
  private val mkvMerge = MkvMergeService(context)
  
  suspend fun start(records: List<ConvertVideo>) {
    try {
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
//        onComplete(e)
      }
    }
  }
  
  suspend fun scanFilesystem(
    frozenSettings: MainSettings,
    previous: List<ConvertVideo>
  ): List<ConvertVideo> {
    val main = frozenSettings.mainSourceProperty.value
    val targetVideoRegex = main.pathPattern.toRegex()
    
    val allExtenstions =  SourceType.entries.flatMap { it.fileExtensions }.toSet()
    
    val outputToCompare = if (settings.outputDir.isAbsolute) {
      if (settings.outputDir.startsWith(settings.workingDir))
        settings.outputDir.relativize(settings.workingDir)
      else
        null
    } else {
      settings.outputDir
    }
    
    val allFiles: List<Path> = Files.walk(frozenSettings.workingDir, frozenSettings.directorySearchDepth).use { stream ->
      val extensions = SourceType.entries.flatMap { it.fileExtensions }
      stream
        .filter { it.isRegularFile() }
        .filter { it.extension in extensions }
        .map { frozenSettings.workingDir.relativize(it) }
        .filter { outputToCompare == null || !it.startsWith(outputToCompare) }
        .toList()
    }
    
    log.info("Processing ${allFiles.size} files")
    
    val mainIndex = frozenSettings.sources.indexOfFirst { it.main }
    
    val results = ConcurrentLinkedQueue<ConvertVideo>()
    allFiles
      .asSequence()
      .filter { it.extension in main.type.fileExtensions }
      .toList()
      .parallelStream()
      .forEach { videoPath ->
        val groupValues = targetVideoRegex.find(videoPath.toString().replace('\\', '/'))?.groupValues ?: return@forEach
        
        val previousVideo = previous.firstOrNull { it.targetVideo == videoPath }
        
        val foundPaths = mutableListOf(videoPath)
        val sourcesRegexes = buildRegexForAdditionalSourcesPaths(groupValues)
        var additionalSources = sourcesRegexes.mapIndexed { i, (regex, exception) ->
          val def = frozenSettings.sources[i]
          val previousAdditionalSource = previousVideo?.sources?.get(i)
          if (!def.enabled || regex.isNullOrBlank()) return@mapIndexed ConvertAdditionalSource(
            null,
            null,
            previousAdditionalSource,
            def
          )
          
          val path = allFiles
            .asSequence()
            .filter { it.extension in def.type.fileExtensions }
            .filterNot { foundPaths.contains(it) }
            .firstOrNull { regex.toRegex().containsMatchIn(it.toString().replace('\\', '/')) }
          
          if (path == null) return@mapIndexed ConvertAdditionalSource(null, null, previousAdditionalSource, def)
          
          
          foundPaths.add(path)
          val metadata = probeFile(path)
          
          val offset = filesystemOffestAttributeRegex.find(path.name)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            ?: Files.list(path.parent?.let { frozenSettings.workingDir.resolve(it) } ?: frozenSettings.workingDir)
              .filter { it.isRegularFile() }
              .filter { it.extension !in allExtenstions }
              .asSequence()
              .firstNotNullOfOrNull { filesystemOffestAttributeRegex.find(it.name)?.groupValues?.getOrNull(1)?.toDoubleOrNull() }
          
          ConvertAdditionalSource(path, metadata, previousAdditionalSource, def, offset)
        }
        
        val state = if (Files.exists(calculateOutput(videoPath))) {
          ConversionProcessState.EXISTS
        } else {
          ConversionProcessState.READY
        }
        val metadata = probeFile(videoPath)
        
        results.add(ConvertVideo(results.size + 1, videoPath, additionalSources, mainIndex, state, metadata))
      }
//    results.parallelStream().forEach {
//      val ffprobe = probeFile(it.targetVideo)
//      if (ffprobe.streams.isNotEmpty()) {
//        it.metadata = VideoMetadata(ffprobe)
//      }
//    }
//    results.flatMap { it.sources }.filter { it.isValid }.parallelStream().forEach {
//      val ffprobe = probeFile(it.path!!)
//      if (ffprobe.streams.isNotEmpty()) {
//        it.metadata = VideoMetadata(ffprobe)
//      }
//    }
    return results.filter { it.metadata != null }
  }
  
  fun buildRegexForAdditionalSourcesPaths(matchedGroupsInVideoPath: List<String>): List<Pair<String?, Exception?>> {
    val values = matchedGroupsInVideoPath.mapIndexed { i, s -> i.toString() to s }.toMap()
    val templater = Strings.getTemplaterWithFormatterSupport(values)
    return settings.sources.map {
      if (it.enabled && it.pathPattern.isNotBlank() && !it.main)
        try {
          templater.replace(it.pathPattern) to null
        } catch (e: Strings.KeyNotFoundException) {
          "=== Matching group ${e.key} not found ===" to e
        } catch (e: Strings.IncorrectConversionException) {
          "=== Conversion '${e.conversion}' contains illegal specifier '${e.specifier}' ===" to e
        }
      else {
        null to null
      }
    }
  }
  
  
  fun calculateOutput(videoPath: Path): Path {
    val videoPart = if (videoPath.isAbsolute) {
      settings.workingDir.relativize(videoPath)
    } else {
      videoPath
    }
    
    val path = videoPart.parent?.resolve("${videoPart.nameWithoutExtension}.${settings.outputExtension}")
      ?: Path.of("${videoPart.nameWithoutExtension}.${settings.outputExtension}")
    
    val out = settings.outputDir.resolve(path)
    if (out.isAbsolute) {
      return out
    }
    return settings.workingDir.resolve(out)
  }
  
  
  fun generateCommand(video: ConvertVideo): List<String> {
    val output = calculateOutput(video.targetVideo)
    return mkvMerge.generateCommand(video, output)
  }
  
  fun probeFile(path: Path): MediaMetadata? {
    try {
      val builder = ProcessBuilder(
        "ffprobe",
        "-v",
        "quiet",
        "-show_format",
        "-show_streams",
        "-output_format",
        "json",
        "\"${settings.workingDir.resolve(path)}\""
      )
        .directory(settings.workingDir.toFile())
        .redirectError(ProcessBuilder.Redirect.INHERIT)
      
      val process = builder.start()
      val output = process.inputStream.bufferedReader().use { it.readText() }
      process.waitFor()
      val ffprobe = SerialisationUtils.fromJson(output, FFProbeResult::class.java)
      if (ffprobe.streams.isNotEmpty()) {
        return MediaMetadata(ffprobe)
      }
      return null
    } catch (e: Exception) {
      log.error("Error probing file $path", e)
    }
    return null
  }
  
  private suspend fun run(records: List<ConvertVideo>) {
    val futures = records.filter { it.state == ConversionProcessState.READY }.mapNotNull { video ->
      val output = calculateOutput(video.targetVideo)
      try {
        Files.createDirectories(output.parent)
        
        if (Files.exists(output)) {
          FileUtils.renameAsBackup(output)
        }
        
        withContext(context.mainDispatcher) {
          video.state = ConversionProcessState.IN_QUEUE
        }
        
        threadPool.submit(Callable {
          runBlocking {
            withContext(context.mainDispatcher) {
              video.state = ConversionProcessState.PROCESSING
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
        
        log.info("Done {}", video.targetVideo)
        
        val state = if (success && Files.exists(calculateOutput(video.targetVideo))) {
          ConversionProcessState.COMPLETED
        } else {
          ConversionProcessState.FAILED
        }
        
        withContext(context.mainDispatcher) {
          video.state = state
        }
        
      } catch (e: Exception) {
        log.error(e.message, e)
        
        withContext(context.mainDispatcher) {
          video.state = ConversionProcessState.FAILED
        }
      }
    }
  }
  
  private suspend fun runSingle(video: ConvertVideo): Boolean {
    var process: Process? = null
    
    try {
      val cmd = generateCommand(video)
      
      log.debug("RUN: {}", cmd.joinToString(" "))
      
      val outputDir = calculateOutput(video.targetVideo).parent
      settings.workingDir.resolve(video.targetVideo).parent.listDirectoryEntries("*.nfo").forEach {
        it.copyTo(outputDir.resolve(it.name), true)
      }
      
      
      val builder = ProcessBuilder(cmd)
        .directory(settings.workingDir.toFile())
//          .inheritIO()
      
      process = builder.start()
      processes.add(process)
      
      var otherLogs: StringBuilder = StringBuilder()
      
      val errors = StringWriter()
      process.errorStream.bufferedReader().use {
        it.transferTo(errors)
      }
      if (errors.buffer.isNotEmpty()) {
        log.error("converter errors: {}", errors.buffer.toString())
      }
      
      if (video.metadata != null) {
        val reader = process.inputStream.bufferedReader()
//        var line = reader.readLine()
//        var lastProcessedMs: Long? = null
//        var lastBitrate: BitTransferSpeed? = null
        otherLogs = mkvMerge.parseOutput(video, reader.lineSequence()) {
          video.progress = it
        }
      }
      val exitCode = process.waitFor()
      if (otherLogs.isNotEmpty()) {
        log.debug("converter logs: \n{}", otherLogs.toString())
      }
      if (exitCode == 0) {
        video.progress = video.progress.copy(percentage = 100.0)
      }
      return exitCode == 0
    } finally {
      if (process != null) {
        processes.remove(process)
      }
    }
  }
  
  fun cancelProcessing() {
    for (process in processes.toList()) {
      try {
        process.destroy()
      } catch (e: Exception) {
        log.error("Cannot kill process", e)
      }
    }
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
