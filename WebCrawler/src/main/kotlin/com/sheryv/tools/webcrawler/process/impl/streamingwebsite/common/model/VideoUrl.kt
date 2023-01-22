package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.sheryv.tools.webcrawler.config.Configuration

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
  JsonSubTypes.Type(value = M3U8Url::class, name = "m3u8"),
  JsonSubTypes.Type(value = DirectUrl::class, name = "direct")
)
sealed interface VideoUrl {
  val base: String
  val isDirect: Boolean
  val isStreaming: Boolean
  
  fun defaultFileFormat() = FileFormats.MP4
  
  fun resolveFileExtension(): String {
    if (Configuration.property("crawler.streaming.episode.get-extension-from-url").toBoolean()) {
      val indexOf = base.lastIndexOf(".")
      if (indexOf > 0 && base.length - indexOf <= 5) {
        return base.substring(indexOf + 1, base.length)
      }
    }
    
    return defaultFileFormat().extension
  }
}

data class M3U8Url(
  override val base: String,
) : VideoUrl {
  init {
    require(base.isNotBlank())
  }
  
  @JsonIgnore
  override val isDirect: Boolean = false
  
  @JsonIgnore
  override val isStreaming: Boolean = true
  
  override fun defaultFileFormat() = FileFormats.TS
}

data class DirectUrl(
  override val base: String,
) : VideoUrl {
  init {
    require(base.isNotBlank())
  }
  
  @JsonIgnore
  override val isDirect: Boolean = true
  
  @JsonIgnore
  override val isStreaming: Boolean = false
}
