package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model


private fun qualityParser(text: String): Qualities {
  val t = text.lowercase()
  return when {
    t.isBlank() -> Qualities.UNKNOWN
    t.contains("1080p") -> Qualities.FULL_HD
    t.contains("720p") -> Qualities.HD
    t.contains("480p") || t.contains("360p") || t.contains("240p") -> Qualities.SD
    else -> Qualities.UNKNOWN
  }
}

data class VideoServerFormat(
  val qualityRawString: String = "",
  val rating: String = "",
  val parser: (String) -> Qualities = ::qualityParser
) {
  val quality by lazy { parser(qualityRawString) }
  
  fun toEpisodeFormat() = if (quality != Qualities.UNKNOWN) EpisodeFormat(rating.takeIf { it.isNotBlank() }, quality) else null
  
  override fun toString() = "${quality}, $rating"
}


enum class Qualities(val label: String, val priority: Int) {
  UHD("Ultra HD - 1440p and above", 0),
  FULL_HD("Full HD - 1080p", 1),
  HD("HD - 720p", 2),
  SD("SD - 480p and below", 3),
  UNKNOWN("Unknown", 100);
  
  override fun toString() = label
  
  fun asSimpleFormat() =  EpisodeFormat("", this)
}
