package com.sheryv.tools.videoconverter.video

import com.fasterxml.jackson.annotation.JsonIgnore
import com.sheryv.tools.videoconverter.video.process.ConversionProcessState
import com.sheryv.tools.videoconverter.video.process.ffprobe.FFProbeResult
import com.sheryv.tools.videoconverter.video.process.ffprobe.FFStream
import com.sheryv.util.fx.lib.listProperty
import com.sheryv.util.fx.lib.mutableProperty
import com.sheryv.util.fx.lib.staticProperty
import com.sheryv.util.unit.BitTransferSpeed
import javafx.beans.property.Property
import javafx.collections.FXCollections
import org.apache.commons.lang3.time.DurationFormatUtils
import java.nio.file.Path
import java.text.DecimalFormat
import kotlin.io.path.extension

private val DEFAULT_DECIMAL_FORMATTER = DecimalFormat("#,##0.#")

class ConvertVideo(
  val number: Int,
  val targetVideo: Path,
  val sources: List<ConvertAdditionalSource>,
  val mainSourceIndex: Int,
  state: ConversionProcessState = ConversionProcessState.READY,
  val metadata: MediaMetadata?
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
  val metadataProperty = staticProperty(metadata)
  
  val commandDependencies: List<Property<*>> = listOf(listProperty(sourcesProperty), metadataProperty)
  
  override fun toString(): String {
    val summary = sources.filterNotNull().joinToString(", ") { "${it.type.code} (${it.timeOffset})" }
    return "#$number $targetVideo [$summary]"
  }
}

enum class SourceType(val code: String, val label: String, val isFilter: Boolean, val fileExtensions: List<String>) {
  VIDEO("V", "Video", true, listOf("mkv", "mp4", "m4v", "ts", "avi", "wmv", "webm", "flv")),
  ALL("*", "ALL (Main)", true, VIDEO.fileExtensions),
  STANDALONE_AUDIO("sA", "Standalone Audio", false, listOf("mp3", "aac", "flac", "m4a", "ac3", "dts")),
  EMBEDDED_AUDIO("eA", "Embedded Audio", false, VIDEO.fileExtensions),
  AUDIO("A", "Audio", true, STANDALONE_AUDIO.fileExtensions + EMBEDDED_AUDIO.fileExtensions),
  STANDALONE_SUBTITLES("sS", "Standalone Subtitles", false, listOf("srt", "vtt")),
  EMBEDDED_SUBTITLES("eS", "Embedded Subtitles", false, listOf("mkv", "mp4", "m4v")),
  SUBTITLES("S", "Subtitles", true, STANDALONE_SUBTITLES.fileExtensions + EMBEDDED_SUBTITLES.fileExtensions),
  ;
  
  fun isAudio() = this == AUDIO || this == STANDALONE_AUDIO || this == EMBEDDED_AUDIO
  fun isSubtitle() = this == SUBTITLES || this == STANDALONE_SUBTITLES || this == EMBEDDED_SUBTITLES
  
  override fun toString() = label
  
  companion object {
    fun findByExtension(extension: String, isFilter: Boolean = false): SourceType? {
      return entries.filter { it.isFilter == isFilter }.firstOrNull { it.fileExtensions.contains(extension) }
    }
  }
}

enum class SourceAudioType(val label: String, val code: String) {
  NOT_SPECIFIED("Not specified", ""),
  DUBBING("Dubbing", "DUB"),
  VOICE_OVER("Voice Over", "VO"),
  ;
  
  override fun toString() = label
}

enum class StreamSelection(val label: String) {
  ALL("All streams"),
  FIRST("Only first"),
  
  ;
  
  override fun toString() = label
}


class ConvertAdditionalSource(
  val type: SourceType,
  val path: Path?,
  timeOffset: Double,
  audioType: SourceAudioType?,
  language: String,
  streamSelection: StreamSelection,
  val definition: SourceSettings,
  val metadata: MediaMetadata?
) {
  
  constructor(path: Path?, metadata: MediaMetadata?, other: ConvertAdditionalSource?, template: SourceSettings) : this(
    path?.let { SourceType.findByExtension(path.extension, false) ?: throw RuntimeException("Unknown extension: $path") } ?: template.type,
    path,
    other?.timeOffset ?: template.defaultTimeOffset,
    other?.audioType ?: template.audioType,
    other?.language ?: template.language,
    other?.streamSelection ?: template.streamSelection,
    template,
    metadata
  )
  
  val isValid = path != null && metadata != null
  
  @JsonIgnore
  val typeProperty = staticProperty(type.toString())
  
  @JsonIgnore
  val pathProperty = staticProperty(path)
  
  @JsonIgnore
  val timeOffsetProperty = mutableProperty(timeOffset)
  var timeOffset by timeOffsetProperty
  
  @JsonIgnore
  val audioTypeProperty = mutableProperty(audioType)
  var audioType by audioTypeProperty
  
  @JsonIgnore
  val languageProperty = mutableProperty(language)
  var language by languageProperty
  
  @JsonIgnore
  val streamSelectionProperty = mutableProperty(streamSelection)
  var streamSelection by streamSelectionProperty
  
  @JsonIgnore
  val metadataProperty = staticProperty(metadata)
}

data class ConversionProgress(val percentage: Double = 0.0, val bitrate: BitTransferSpeed = BitTransferSpeed.zero) {
  val formatted = format()
  
  private fun format(): String {
    if (percentage == 0.0) {
      return "-"
    }
    
    val percent = DEFAULT_DECIMAL_FORMATTER.format(percentage)
    if (bitrate.value != 0.0) {
      return "$percent % | ${bitrate.formatted}"
    }
    
    return "$percent %"
  }
}

class MediaMetadata(
  ffprobe: FFProbeResult
) {
  val streams = ffprobe.streams
  val nonVideoStreams = streams.filter { it.codecType == "audio" || it.codecType == "subtitle" }
  
  val durationMs = (ffprobe.format?.duration?.toDouble()?.times(1_000.0))?.toLong() ?: 0
  val bitrate = BitTransferSpeed.calc(ffprobe.format?.bitRate?.toLong() ?: 0)
  val formatName = ffprobe.format?.formatLongName.orEmpty()
  
  val video = streams.firstOrNull { it.codecType == "video" }?.let { VideoMetadata(it) }
  
  fun streamsForType(type: SourceType, streams: Collection<FFStream> = this.streams): List<FFStream> {
    return when {
      type == SourceType.VIDEO -> streams.filter { it.codecType == "video" }
      type.isAudio() -> streams.filter { it.codecType == "audio" }
      type.isSubtitle() -> streams.filter { it.codecType == "subtitle" }
      else -> throw IllegalArgumentException("Unsupported stream type: $type")
    }
  }
  
  
  fun format(): String {
    val durationString = DurationFormatUtils.formatDuration(durationMs, "HH:mm:ss.SSS", true)
    val line = if (video != null) {
      "${video.resolution} - ${video.codec} - ${bitrate.formatted} - ${video.fps} fps"
    } else {
      bitrate.formatted
    }
    return """
      |$durationString  -  $formatName
      |$line
    """.trimMargin()
  }
}

class VideoMetadata(
  video: FFStream
) {
  val fps = video.avgFrameRate?.split('/')?.first()?.toDouble()?.let {
    if (it > 1000) it / 1000.0 else it
  }
  val resolution = "${video.width}x${video.height}"
  val codec = video.codecName
}
