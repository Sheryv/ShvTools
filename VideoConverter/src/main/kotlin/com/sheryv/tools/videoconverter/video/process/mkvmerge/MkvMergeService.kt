package com.sheryv.tools.videoconverter.video.process.mkvmerge

import com.sheryv.tools.videoconverter.Context
import com.sheryv.tools.videoconverter.video.*
import com.sheryv.tools.videoconverter.video.process.Language
import org.apache.commons.lang3.time.DurationFormatUtils
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class MkvMergeService(private val context: Context) {
  
  fun generateCommand(video: ConvertVideo, outputPath: Path): List<String> {
    val sources = video.sources.filter { it.isValid }
    val cmd = mutableListOf("mkvmerge", "--title", quote(outputPath.nameWithoutExtension), "--gui-mode", "-o", quote(outputPath))
    
    require(video.metadata != null) { "Metadata not found for type ${video.targetVideo}" }
    
    if (sources.isNotEmpty()) {
      cmd += "--split"
      cmd += "parts:00:00:00-" + DurationFormatUtils.formatDuration(video.metadata.durationMs, "HH:mm:ss.SSS", true)
    }
    
    val order = mutableListOf<Pair<Int, Int>>()
    
    val mainFileTracks = video.metadata.streams.filter { context.isLanguageSupported(it.language()) }

    val toSkip = video.metadata.streams.subtract(mainFileTracks)
    
    listOf(SourceType.VIDEO, SourceType.AUDIO, SourceType.SUBTITLES).forEach {
      val skipped = video.metadata.streamsForType(it, toSkip)
      if (skipped.isNotEmpty()) {
        cmd += "-" + typeToTrackCodeName(it)
        cmd += skipped.joinToString(",") { "!" + it.index.toString() }
      }
    }
    
    cmd += quote(video.targetVideo)
    
    sources.forEachIndexed { index, source ->
      if (index == video.mainSourceIndex){
        mainFileTracks.forEach {
          order += 0 to it.index
        }
      }
      
      cmd += "-D"
      cmd += "-B"
      cmd += "-M"
      cmd += "--no-chapters"
      
      if (source.type.isAudio()) {
        cmd += "-S"
      } else if (source.type.isSubtitle()) {
        cmd += "-A"
      }
      
      var streams: List<Pair<Int, Language?>> = if (source.metadata != null) {
        source.metadata.streamsForType(source.type)
          .filter { context.isLanguageSupported(it.language()) }
          .map { it.index to it.language()?.let { context.findLanguage(it) } }
          .ifEmpty { listOf(0 to null) }
      } else {
        throw RuntimeException("Metadata not found for type ${source.path}")
      }
      
      val userLang = context.findLanguage(source.definition.language).takeIf { source.definition.language.isNotBlank() }
      
      if (source.streamSelection == StreamSelection.FIRST) {
        streams = streams.take(1)
      }
//      if (userLang != null) {
//        cmd += "--language"
//        cmd += "0:${userLang.code}"
//      }

//      if (streams != null) {
      cmd += "-" + typeToTrackCodeName(source.type)
      
      cmd += streams.joinToString(",") { "${it.first}" }
//      }
      
      streams.forEach { (stream, language) ->
        order += index + 1 to stream
        
        if (source.timeOffset != 0.0) {
//        longestOffset = longestOffset.coerceAtLeast(-source.timeOffset)
          cmd += "--sync"
          cmd += "$stream:${(source.timeOffset * 1000).toLong()}"
        }
        
        if (language != null || userLang != null) {
          val selectedLang = language ?: userLang
          var suffix = ""
          if (source.audioType != null && source.audioType != SourceAudioType.NOT_SPECIFIED) {
            suffix = " [${source.audioType!!.code}]"
          }
          
          cmd += "--language"
          cmd += "$stream:${selectedLang!!.code}"
          cmd += "--track-name"
          cmd += quote("$stream:${selectedLang.originalName}$suffix")
        }
      }
      
      cmd += quote(source.path!!)

//      lastOptions.add("-map")
//      lastOptions.add("${index + 1}:$code")
//      if (source.definition.language.isNotBlank()) {
//        lastOptions.add("-metadata:s:$code:${processedByCode[code] ?: 0}")
//        lastOptions.add("language=${source.definition.language}")
//        if (source.definition.language in context.languages) {
//          var suffix = ""
//          if (source.audioType != null) {
//            suffix = " [${source.audioType!!.code}]"
//          }
//
//          lastOptions.add("-metadata:s:$code:${processedByCode[code] ?: 0}")
//          lastOptions.add("title=${context.languages[source.definition.language]!!.originalName}$suffix")
//        }
//      }
//
    }
    if (order.isNotEmpty()) {
      if (order.none { it.first == 0 }){
        mainFileTracks.forEach {
          order += 0 to it.index
        }
      }
      
      cmd += "--track-order"
      cmd += order.joinToString(",") { "${it.first}:${it.second}" }
    }
    return cmd
  }
  
  fun parseOutput(video: ConvertVideo, lines: Sequence<String>, progress: (ConversionProgress) -> Unit): StringBuilder {
//    ConversionProgress(lastProcessedMs / video.metadata!!.durationMs.toDouble() * 100, lastBitrate)
    val otherLogs = StringBuilder()
    
    lines.forEach { lineRaw ->
      val line = lineRaw.trim()
      if (line.startsWith("#GUI#progress")) {
        val parts = line.split(' ')
        if (parts.size > 1) {
          val percent = parts[1].replace("%", "").toDoubleOrNull()
          if (percent != null) {
            progress(ConversionProgress(percent))
            return@forEach
          }
        }
      }
      otherLogs.appendLine(line)
    }

//    "#GUI#progress 10%"
    
    return otherLogs
  }
  
  private fun quote(value: Any) = '"' + value.toString() + '"'
  
  private fun typeToTrackCodeName(type: SourceType): String {
    return when {
      type.isAudio() -> "a"
      type.isSubtitle() -> "s"
      else -> "d"
    }
  }
}
