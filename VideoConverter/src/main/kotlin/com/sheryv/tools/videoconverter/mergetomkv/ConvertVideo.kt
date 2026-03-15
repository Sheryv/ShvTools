package com.sheryv.tools.videoconverter.mergetomkv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.videoconverter.mergetomkv.ffprobe.FFProbeResult
import com.sheryv.util.fx.lib.mutableProperty
import com.sheryv.util.fx.lib.staticProperty
import com.sheryv.util.unit.BitTransferSpeed
import javafx.collections.FXCollections
import org.apache.commons.lang3.time.DurationFormatUtils
import java.nio.file.Path
import java.text.DecimalFormat

private val DEFAULT_DECIMAL_FORMATTER = DecimalFormat("#,##0.0")

class ConvertVideo(
  val number: Int,
  val targetVideo: Path,
  val sources: List<ConvertAdditionalSource?>,
  state: ConversionProcessState = ConversionProcessState.READY,
) {
  @JsonIgnore
  val stateProperty = mutableProperty(state)
  var state by stateProperty
  
  @JsonIgnore
  val numberProperty = staticProperty(number)
  
  @JsonIgnore
  val targetVideoProperty = staticProperty(targetVideo.toString())
  
  @JsonIgnore
  val sourcesProperty = FXCollections.observableArrayList(sources)
  
  @JsonIgnore
  val progressProperty = mutableProperty(ConversionProgress())
  var progress by progressProperty
  
  @JsonIgnore
  val metadataProperty = mutableProperty<VideoMetadata?>(null)
  var metadata by metadataProperty
}

enum class SourceType(val code: String, val fileExtensions: List<String>) {
  AUDIO("A", listOf("mp3", "aac")),
  VIDEO("A", listOf("mkv", "mp4", "m4v", "ts", "avi")),
  SUBTITLES("S", listOf("srt", "vtt"));
  
  companion object {
    fun findByExtension(extension: String): SourceType? {
      return SourceType.entries.firstOrNull { it.fileExtensions.contains(extension) }
    }
  }
}


class ConvertAdditionalSource(val type: SourceType, val path: Path, timeOffset: Double, val definition: SourceSettings) {
  @JsonIgnore
  val typeProperty = staticProperty(type.toString())
  
  @JsonIgnore
  val pathProperty = staticProperty(path.toString())
  
  @JsonIgnore
  val timeOffsetProperty = mutableProperty(timeOffset)
  
  var timeOffset by timeOffsetProperty
}

data class ConversionProgress(val percentage: Double = 0.0, val bitrate: BitTransferSpeed = BitTransferSpeed.zero) {
  val formatted = format()
  
  private fun format(): String {
    if (percentage == 0.0) {
      return "-"
    }
    
    return "${DEFAULT_DECIMAL_FORMATTER.format(percentage)} % | ${bitrate.formatted}"
  }
}

class VideoMetadata(
  ffprobe: FFProbeResult
) {
  private val video = ffprobe.streams.first { it.codecType == "video" }
  val durationMs = (ffprobe.format.duration.toDouble() * 1_000.0).toLong()
  val bitrate = BitTransferSpeed.calc(ffprobe.format.bitRate.toLong())
  val fps = video.avgFrameRate.split('/').first().toDouble()
  val resolution = "${video.width}x${video.height}"
  val codec = video.codecName
  val formatName = ffprobe.format.formatLongName
  
  fun format(): String {
    val durationString = DurationFormatUtils.formatDuration(durationMs, "HH:mm:ss.SSS", true)
    return """
      |$durationString  -  $formatName
      |$resolution - $codec - ${bitrate.formatted} - $fps fps
    """.trimMargin()
  }
}
