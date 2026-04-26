package com.sheryv.tools.videoconverter.video

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.videoconverter.video.process.ConversionProcessState
import com.sheryv.tools.videoconverter.video.process.ffprobe.FFProbeResult
import com.sheryv.util.fx.lib.listProperty
import com.sheryv.util.fx.lib.mutableProperty
import com.sheryv.util.fx.lib.staticProperty
import com.sheryv.util.unit.BitTransferSpeed
import javafx.beans.property.Property
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
  
  val commandDependencies: List<Property<*>> = listOf(listProperty(sourcesProperty), metadataProperty)
  
  override fun toString(): String {
    val summary = sources.filterNotNull().joinToString(", ") { "${it.type.code} (${it.timeOffset})" }
    return "#$number $targetVideo [$summary]"
  }
}

enum class SourceType(val code: String, val label: String, val isFilter: Boolean, val fileExtensions: List<String>) {
  VIDEO("V", "Video", true, listOf("mkv", "mp4", "m4v", "ts", "avi", "wmv", "webm", "flv")),
  STANDALONE_AUDIO("sA", "Standalone Audio", false, listOf("mp3", "aac", "flac", "m4a", "ac3", "dts")),
  EMBEDDED_AUDIO("eA", "Embedded Audio", false, VIDEO.fileExtensions),
  AUDIO("A", "Audio", true, STANDALONE_AUDIO.fileExtensions + EMBEDDED_AUDIO.fileExtensions),
  STANDALONE_SUBTITLES("sS", "Standalone Subtitles", false, listOf("srt", "vtt")),
  EMBEDDED_SUBTITLES("eS", "Embedded Subtitles", false, listOf("mkv", "mp4", "m4v")),
  SUBTITLES("S", "Subtitles", true, STANDALONE_SUBTITLES.fileExtensions + EMBEDDED_SUBTITLES.fileExtensions),
  ;
  
  override fun toString() = label
  
  companion object {
    fun findByExtension(extension: String, isFilter: Boolean = false): SourceType? {
      return entries.filter { it.isFilter == isFilter }.firstOrNull { it.fileExtensions.contains(extension) }
    }
  }
}

enum class SourceAudioType(val label: String, val code: String) {
  DUBBING("Dubbing", "DUB"),
  VOICE_OVER("Voice Over", "VO"),
  ;
  
  override fun toString() = label
}


class ConvertAdditionalSource(
  val type: SourceType,
  val path: Path,
  timeOffset: Double,
  audioType: SourceAudioType?,
  language: String,
  val definition: SourceSettings
) {
  @JsonIgnore
  val typeProperty = staticProperty(type.toString())
  
  @JsonIgnore
  val pathProperty = staticProperty(path.toString())
  
  @JsonIgnore
  val timeOffsetProperty = mutableProperty(timeOffset)
  var timeOffset by timeOffsetProperty
  
  @JsonIgnore
  val audioTypeProperty = mutableProperty(audioType)
  var audioType by audioTypeProperty
  
  @JsonIgnore
  val languageProperty = mutableProperty(language)
  var language by languageProperty
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
  val streams = ffprobe.streams
  private val video = streams.first { it.codecType == "video" }
  val durationMs = (ffprobe.format?.duration?.toDouble()?.times(1_000.0))?.toLong() ?: 0
  val bitrate = BitTransferSpeed.calc(ffprobe.format?.bitRate?.toLong() ?: 0)
  val fps = video.avgFrameRate?.split('/')?.first()?.toDouble()?.let {
    if (it > 1000) it / 1000.0 else it
  }
  val resolution = "${video.width}x${video.height}"
  val codec = video.codecName
  val formatName = ffprobe.format?.formatLongName.orEmpty()
  
  fun format(): String {
    val durationString = DurationFormatUtils.formatDuration(durationMs, "HH:mm:ss.SSS", true)
    return """
      |$durationString  -  $formatName
      |$resolution - $codec - ${bitrate.formatted} - $fps fps
    """.trimMargin()
  }
}
