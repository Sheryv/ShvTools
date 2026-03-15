package com.sheryv.tools.videoconverter.mergetomkv.ffprobe

import com.fasterxml.jackson.annotation.JsonProperty

data class FFProbeResult(val format: FFProbeFormat, val streams: List<FFProbeStream>)

data class FFProbeStream(
  val index: Int,
  val width: Int,
  val height: Int,
  @JsonProperty("codec_name")
  val codecName: String,
  @JsonProperty("codec_type")
  val codecType: String,
  @JsonProperty("avg_frame_rate")
  val avgFrameRate: String,
)

data class FFProbeFormat(
  @JsonProperty("format_long_name")
  val formatLongName: String,
  val duration: String,
  @JsonProperty("bit_rate")
  val bitRate: String,
)
