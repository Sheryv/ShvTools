package com.sheryv.tools.videoconverter.video.process.ffmpeg

import com.sheryv.tools.videoconverter.Context
import com.sheryv.tools.videoconverter.video.ConversionProgress
import com.sheryv.tools.videoconverter.video.ConvertVideo
import com.sheryv.tools.videoconverter.video.SourceType
import com.sheryv.util.unit.BitTransferSpeed
import com.sheryv.util.unit.BitUnit
import okio.Path
import kotlin.text.appendLine

class FFmpegService(private val context: Context) {
  
  fun generateCommand(video: ConvertVideo, outputPath: Path): List<String> {
    val sources = video.sources.filter { it.isValid }
//    var audioProcessed = 0
//    var subtitlesProcessed = 0
    
    val processedByCode = mutableMapOf<String, Int>()
    
    val cmd = mutableListOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-progress", "pipe:1", "-i", "\"${video.targetVideo}\"")
    val lastOptions = mutableListOf<String>()
    var longestOffset = 0.0
    sources.forEachIndexed { index, source ->
      
      if (source.timeOffset != 0.0) {
        longestOffset = longestOffset.coerceAtLeast(-source.timeOffset)
        
        cmd.add("-itsoffset")
        cmd.add("${source.timeOffset}")
      }
      val code = typeToFFmpegCode(source.type)
      
      
      lastOptions.add("-map")
      lastOptions.add("${index + 1}:$code")
      if (source.definition.language.isNotBlank()) {
        lastOptions.add("-metadata:s:$code:${processedByCode[code] ?: 0}")
        lastOptions.add("language=${source.definition.language}")
        val lang = context.findLanguage(source.definition.language)
        if (lang != null) {
          
          var suffix = ""
          if (source.audioType != null) {
            suffix = " [${source.audioType!!.code}]"
          }
          
          lastOptions.add("-metadata:s:$code:${processedByCode[code] ?: 0}")
          lastOptions.add("title=${lang.originalName}$suffix")
        }
      }
      processedByCode.compute(code) { _, c -> c?.plus(1) ?: 0 }


//      when (source.type) {
//        SourceType.SUBTITLES -> {
//          lastOptions.add("-map")
//          lastOptions.add("${index + 1}:s")
//          if (source.definition.language.isNotBlank()) {
//            lastOptions.add("-metadata:s:s:${subtitlesProcessed}")
//            lastOptions.add("language=${source.definition.language}")
//            if (source.definition.language in languages) {
//              lastOptions.add("-metadata:s:s:${subtitlesProcessed}")
//              lastOptions.add("title=${languages[source.definition.language]!!.originalName}")
//            }
//          }
//          subtitlesProcessed++
//        }
//
//        SourceType.AUDIO, SourceType.VIDEO -> {
//          lastOptions.add("-map")
//          lastOptions.add("${index + 1}:a:0")
//          if (source.definition.language.isNotBlank()) {
//            lastOptions.add("-metadata:s:a:${audioProcessed}")
//            lastOptions.add("language=${source.definition.language}")
//            if (source.definition.language in languages) {
//              lastOptions.add("-metadata:s:a:${subtitlesProcessed}")
//              lastOptions.add("title=${languages[source.definition.language]!!.originalName}")
//            }
//          }
//          audioProcessed++
//        }
//      }
      cmd.add("-i")
      cmd.add("\"${source.path}\"")
    }
    cmd.add("-map")
    cmd.add("0:v")
    cmd.addAll(lastOptions)
    cmd.add("-map")
    cmd.add("0:a")
    cmd.add("-map")
    cmd.add("0:s")
    cmd.add("-c")
    cmd.add("copy")
    
    if (video.metadata != null && sources.any { it.timeOffset != 0.0 }) {
      cmd.add("-ss")
      cmd.add("0")
      cmd.add("-t")
      cmd.add((video.metadata!!.durationMs.toDouble() / 1000.0).toString())
    }
    cmd.add("\"$outputPath\"")
    return cmd
  }
  
  fun parseOutput(video: ConvertVideo, lines: Sequence<String>, progress: (ConversionProgress) -> Unit): StringBuilder {
//    ConversionProgress(lastProcessedMs / video.metadata!!.durationMs.toDouble() * 100, lastBitrate)
    val otherLogs = StringBuilder()
    var lastProcessedMs: Long? = null
    var lastBitrate: BitTransferSpeed? = null
    lines.forEach { line ->
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
      } else {
        otherLogs.appendLine(line)
      }
      
      if (lastBitrate != null && lastProcessedMs != null) {
        progress(ConversionProgress(lastProcessedMs / video.metadata!!.durationMs.toDouble() * 100, lastBitrate))
      }
      
    }
    
    return otherLogs
  }
  
  
  private fun typeToFFmpegCode(type: SourceType): String {
    return when (type) {
      SourceType.SUBTITLES, SourceType.STANDALONE_SUBTITLES, SourceType.EMBEDDED_SUBTITLES -> "s"
      SourceType.EMBEDDED_AUDIO, SourceType.STANDALONE_AUDIO, SourceType.AUDIO -> "a"
      else -> "a"
    }
  }
}
