package com.sheryv.tools.videoconverter.mergetomkv

import com.sheryv.tools.videoconverter.mergetomkv.ffprobe.FFProbeResult
import com.sheryv.util.SerialisationUtils
import com.sheryv.util.Strings
import com.sheryv.util.inMainContext
import com.sheryv.util.inMainThread
import com.sheryv.util.logging.log
import com.sheryv.util.unit.BitTransferSpeed
import com.sheryv.util.unit.BitUnit
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

class ConversionProcessor(
  private val settings: ConverterSettings,
  private val onProgress: (String) -> Unit,
  private val onComplete: (Exception?) -> Unit
) : AutoCloseable {
  
  private val processes: Queue<Process> = ConcurrentLinkedQueue()
  private val threadPool = Executors.newFixedThreadPool(settings.parallelProcessing)
  
  suspend fun start(records: List<ConvertVideo>) {
    try {
      inMainContext {
        onProgress("Processing videos...")
      }
      
      run(records)
      
      inMainContext {
        onProgress("Completed")
        onComplete(null)
      }
    } catch (e: Exception) {
      inMainContext {
        onProgress("Completed")
        onComplete(e)
      }
    }
  }
  
  suspend fun scanFilesystem(
    frozenSettings: ConverterSettings,
    previous: List<ConvertVideo>
  ): List<ConvertVideo> {
    val targetVideoRegex = frozenSettings.videoPathPattern.toRegex()
    
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
    
    val results = mutableListOf<ConvertVideo>()
    allFiles
      .asSequence()
      .filter { it.extension in SourceType.VIDEO.fileExtensions }
      .forEach { videoPath ->
        val groupValues = targetVideoRegex.find(videoPath.toString().replace('\\', '/'))?.groupValues ?: return@forEach
        
        val previousVideo = previous.firstOrNull { it.targetVideo == videoPath }
        
        val foundPaths = mutableListOf<Path>()
        val sourcesRegexes = buildRegexForAdditionalSourcesPaths(groupValues)
        var additionalSources = sourcesRegexes.mapIndexed { i, (regex, exception) ->
          if (regex.isNullOrBlank()) return@mapIndexed null
          
          val path = allFiles
            .asSequence()
            .filter { it.extension in SourceType.AUDIO.fileExtensions || it.extension in SourceType.SUBTITLES.fileExtensions }
            .filterNot { foundPaths.contains(it) }
            .firstOrNull { regex.toRegex().containsMatchIn(it.toString().replace('\\', '/')) }
          
          if (path == null) return@mapIndexed null
          
          val previousAdditionalSource = previousVideo?.sources?.firstOrNull { it?.path == path }
          
          foundPaths.add(path)
          
          ConvertAdditionalSource(
            SourceType.findByExtension(path.extension) ?: throw RuntimeException("Unknown extension: $path"),
            path,
            previousAdditionalSource?.timeOffset ?: frozenSettings.sources[i].defaultTimeOffset,
            frozenSettings.sources[i]
          )
        }
        
        val state = if (Files.exists(calculateOutput(videoPath))) {
          ConversionProcessState.EXISTS
        } else {
          ConversionProcessState.READY
        }
        
        results.add(ConvertVideo(results.size + 1, videoPath, additionalSources, state))
      }
    results.parallelStream().forEach {
      it.metadata = VideoMetadata(probeFile(it.targetVideo))
    }
    return results
  }
  
  fun buildRegexForAdditionalSourcesPaths(matchedGroupsInVideoPath: List<String>): List<Pair<String?, Exception?>> {
    val values = matchedGroupsInVideoPath.mapIndexed { i, s -> i.toString() to s }.toMap()
    val templater = Strings.getTemplaterWithFormatterSupport(values)
    return settings.sources.map {
      if (it.pathPattern.isNotBlank())
        try {
          templater.replace(it.pathPattern) to null
        } catch (e: Strings.KeyNotFoundException) {
          "=== Matching group ${e.key} not found ===" to e
        } catch (e: Strings.IncorrectConverstionException) {
          "=== Conversion '${e.conversion}' contains illegal specifier '${e.specifier}' ===" to e
        }
      else {
        null to null
      }
    }
  }
  
  fun convertSubtitlesEncoding(records: List<ConvertVideo>) {
    for (video in records) {
      video.sources.filterNotNull().filter { it.type == SourceType.SUBTITLES }.forEach {
        if (it.definition.textFileEncoding != StandardCharsets.UTF_8.name()) {
          val path = settings.workingDir.resolve(it.path)
          val subtitlesContent = Files.readString(path, Charset.forName(it.definition.textFileEncoding))
          var backup = path.resolveSibling(path.fileName.toString() + ".0.backup")
          var counter = 1
          while (Files.exists(backup)) {
            backup = path.resolveSibling(path.fileName.toString() + ".${counter}.backup")
            counter++
          }
          Files.move(path, backup)
          Files.writeString(path, subtitlesContent, StandardCharsets.UTF_8)
        }
      }
    }
  }
  
  fun calculateOutput(videoPath: Path): Path {
    val videoPart = if (videoPath.isAbsolute) {
      settings.workingDir.relativize(videoPath)
    } else {
      videoPath
    }
    
    val path = videoPart.parent?.resolve("${videoPart.nameWithoutExtension}.mkv") ?: Path.of("${videoPart.nameWithoutExtension}.mkv")
    
    val out = settings.outputDir.resolve(path)
    if (out.isAbsolute) {
      return out
    }
    return settings.workingDir.resolve(out)
  }
  
  fun generateCommand(video: ConvertVideo): List<String> {
    val output = calculateOutput(video.targetVideo)
    val sources = video.sources.filterNotNull()
    var audioProcessed = 1
    var subtitlesProcessed = 0
    
    val cmd = mutableListOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-progress", "pipe:1", "-i", "\"${video.targetVideo}\"")
    val lastOptions = mutableListOf<String>()
    var longestOffset = 0.0
    sources.forEachIndexed { index, source ->
      if (source.timeOffset != 0.0) {
        longestOffset = longestOffset.coerceAtLeast(-source.timeOffset)
        
        cmd.add("-itsoffset")
        cmd.add("${source.timeOffset}")
      }
      when (source.type) {
        SourceType.SUBTITLES -> {
          lastOptions.add("-map")
          lastOptions.add("${index + 1}:s")
          if (source.definition.language.isNotBlank()) {
            lastOptions.add("-metadata:s:s:${subtitlesProcessed}")
          }
//          cmd.add("-c:s")
//          cmd.add("copy")
          subtitlesProcessed++
        }
        
        SourceType.AUDIO, SourceType.VIDEO -> {
          lastOptions.add("-map")
          lastOptions.add("${index + 1}:a")
          if (source.definition.language.isNotBlank()) {
            lastOptions.add("-metadata:s:a:${audioProcessed}")
          }
          cmd.add("-c:a")
          cmd.add("copy")
          audioProcessed++
        }
      }
      cmd.add("-i")
      cmd.add("\"${source.path}\"")
      if (source.definition.language.isNotBlank()) {
        lastOptions.add("language=${source.definition.language}")
      }
    }
    cmd.add("-map")
    cmd.add("0")
    cmd.addAll(lastOptions)
    cmd.add("-c")
    cmd.add("copy")
    
    cmd.add("-ss")
    cmd.add("0")
    if (video.metadata != null) {
      cmd.add("-t")
      cmd.add((video.metadata!!.durationMs.toDouble() / 1000.0).toString())
    }
    cmd.add("\"$output\"")
    return cmd
  }
  
  fun probeFile(path: Path): FFProbeResult {
    
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
    return SerialisationUtils.fromJson(output, FFProbeResult::class.java)
  }
  
  private suspend fun run(records: List<ConvertVideo>) {
    val futures = records.filter { it.state == ConversionProcessState.READY }.mapNotNull { video ->
      val output = calculateOutput(video.targetVideo)
      try {
        Files.createDirectories(output.parent)
        
        inMainContext {
          video.state = ConversionProcessState.IN_QUEUE
        }
        
        threadPool.submit(Callable {
          inMainThread {
            video.state = ConversionProcessState.PROCESSING
          }
          
          runSingle(video)
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
        
        inMainContext {
          video.state = state
        }
        
      } catch (e: Exception) {
        log.error(e.message, e)
        
        inMainContext {
          video.state = ConversionProcessState.FAILED
        }
      }
    }
  }
  
  private fun runSingle(video: ConvertVideo): Boolean {
    var process: Process? = null
    
    try {
      val cmd = generateCommand(video)
      
      log.debug("RUN: {}", cmd.joinToString(" "))
      
      val builder = ProcessBuilder(cmd)
        .directory(settings.workingDir.toFile())
        .redirectErrorStream(true)
//          .inheritIO()
      
      process = builder.start()
      processes.add(process)
      
      if (video.metadata != null) {
        val reader = process.inputStream.bufferedReader()
        var line = reader.readLine()
        var lastProcessedMs: Long? = null
        var lastBitrate: BitTransferSpeed? = null
        while (line != null) {
          
          if (line.startsWith("out_time_us")) {
            lastProcessedMs = line.split('=').getOrNull(1)?.toLongOrNull()?.div(1000)
          } else if (line.startsWith("bitrate")) {
            lastBitrate = line.split('=').getOrNull(1)?.let {
              var value = 0.0
              var unit = BitUnit.b
              
              when {
                it.endsWith("kbits/s") -> {
                  value = it.dropLast(7).trim().toDouble()
                  unit = BitUnit.kb
                }
                
                it.lowercase().endsWith("mbits/s") -> {
                  value = it.dropLast(7).trim().toDouble()
                  unit = BitUnit.Mb
                }
                
                it.lowercase().endsWith("bits/s") -> {
                  value = it.dropLast(6).trim().toDouble()
                }
              }
              
              BitTransferSpeed.calc(value, unit)
            }
          }
          
          if (lastBitrate != null && lastProcessedMs != null) {
            video.progress = ConversionProgress(lastProcessedMs / video.metadata!!.durationMs.toDouble() * 100, lastBitrate)
          }
          
          line = reader.readLine()
        }
        reader.close()
      }
      val exitCode = process.waitFor()
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
