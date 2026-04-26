package com.sheryv.tools.videoconverter.video.process.ffprobe

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class FFProbeResult(val format: FFFormat?, val streams: List<FFStream> = listOf(), val chapters: List<FFChapter>? = null)


@JsonIgnoreProperties(ignoreUnknown = true)
data class FFStream(
  val index: Int,
  @JsonProperty("codec_name") val codecName: String?,
  @JsonProperty("codec_long_name") val codecLongName: String?,
  @JsonProperty("codec_type") val codecType: String?,
  @JsonProperty("codec_tag_string") val codecTagString: String?,
  @JsonProperty("codec_tag") val codecTag: String?,
  
  // Video specific
  val width: Int?,
  val height: Int?,
  @JsonProperty("coded_width") val codedWidth: Int?,
  @JsonProperty("coded_height") val codedHeight: Int?,
  @JsonProperty("has_b_frames") val hasBFrames: Int?,
  @JsonProperty("sample_aspect_ratio") val sampleAspectRatio: String?,
  @JsonProperty("display_aspect_ratio") val displayAspectRatio: String?,
  @JsonProperty("pix_fmt") val pixFmt: String?,
  val level: Int?,
  val profile: String?,
  @JsonProperty("color_range") val colorRange: String?,
  @JsonProperty("color_space") val colorSpace: String?,
  @JsonProperty("color_transfer") val colorTransfer: String?,
  @JsonProperty("color_primaries") val colorPrimaries: String?,
  @JsonProperty("field_order") val fieldOrder: String?,
  @JsonProperty("refs") val refs: Int?,
  
  // Audio specific
  @JsonProperty("sample_fmt") val sampleFmt: String?,
  @JsonProperty("sample_rate") val sampleRate: String?,
  val channels: Int?,
  @JsonProperty("channel_layout") val channelLayout: String?,
  @JsonProperty("bits_per_sample") val bitsPerSample: Int?,
  
  // Timing and Frame Rate
  @JsonProperty("r_frame_rate") val rFrameRate: String?,
  @JsonProperty("avg_frame_rate") val avgFrameRate: String?,
  @JsonProperty("time_base") val timeBase: String?,
  @JsonProperty("start_pts") val startPts: Long?,
  @JsonProperty("start_time") val startTime: String?,
  @JsonProperty("duration_ts") val durationTs: Long?,
  val duration: String?,
  @JsonProperty("bit_rate") val bitRate: String?,
  @JsonProperty("nb_frames") val nbFrames: String?,
  
  // Metadata
  val disposition: FFDisposition? = null,
  val tags: Map<String, String>? = null,
  @JsonProperty("side_data_list") val sideDataList: List<Map<String, Any>>? = null
) {
  fun language(): String? {
    return tags?.get("language")
  }
  
  fun title(): String? {
    return tags?.get("title")
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FFFormat(
  val filename: String?,
  @JsonProperty("nb_streams") val nbStreams: Int,
  @JsonProperty("nb_programs") val nbPrograms: Int?,
  @JsonProperty("format_name") val formatName: String?,
  @JsonProperty("format_long_name") val formatLongName: String?,
  @JsonProperty("start_time") val startTime: String?,
  val duration: String?,
  val size: String?,
  @JsonProperty("bit_rate") val bitRate: String?,
  @JsonProperty("probe_score") val probeScore: Int?,
  val tags: Map<String, String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FFDisposition(
  val default: Int = 0,
  val dub: Int = 0,
  val original: Int = 0,
  val comment: Int = 0,
  val lyrics: Int = 0,
  val karaoke: Int = 0,
  val forced: Int = 0,
  @JsonProperty("hearing_impaired") val hearingImpaired: Int = 0,
  @JsonProperty("visual_impaired") val visualImpaired: Int = 0,
  @JsonProperty("clean_effects") val cleanEffects: Int = 0,
  @JsonProperty("attached_pic") val attachedPic: Int = 0,
  @JsonProperty("timed_thumbnails") val timedThumbnails: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FFChapter(
  val id: Long,
  @JsonProperty("time_base") val timeBase: String?,
  @JsonProperty("start") val start: Long?,
  @JsonProperty("start_time") val startTime: String?,
  @JsonProperty("end") val end: Long?,
  @JsonProperty("end_time") val endTime: String?,
  val tags: Map<String, String>? = null
)
