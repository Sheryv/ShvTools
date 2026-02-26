package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.sheryv.tools.webcrawler.config.Configuration
import com.sheryv.util.Strings
import java.io.ByteArrayInputStream

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
  JsonSubTypes.Type(value = M3U8Url::class, name = "m3u8"),
  JsonSubTypes.Type(value = DirectUrl::class, name = "direct")
)
sealed class VideoUrl(
  val isStreaming: Boolean,
  open val url: String,
  open val metadata: UrlMetadata
) {
  
  init {
    require(url.isNotBlank())
  }
  
  open val isDirect: Boolean = !isStreaming
  
  open fun defaultFileFormat() = FileFormats.MP4
  
  open fun resolveFileExtension(): String {
    if (Configuration.property("crawler.streaming.episode.get-extension-from-url").toBoolean()) {
      val indexOf = url.lastIndexOf(".")
      if (indexOf > 0 && url.length - indexOf <= 5) {
        return url.substring(indexOf + 1, url.length)
      }
    }
    
    return defaultFileFormat().extension
  }
  
  @JsonIgnore
  fun isSameUrl(other: String): Boolean {
    return other.replaceAfter('?', "").removeSuffix("?")
      .equals(this.url.replaceAfter('?', "").removeSuffix("?"), true)
  }
  
  fun toId(): String {
    return Strings.buildInHash(ByteArrayInputStream(url.encodeToByteArray()), bufferSize = 128)
  }
  
  override fun toString() = url
}

data class UrlMetadata(val headers: Map<String, String> = emptyMap())

class M3U8Url(url: String, metadata: UrlMetadata = UrlMetadata()) : VideoUrl(true, url, metadata) {
  override fun defaultFileFormat() = FileFormats.TS
}

class DirectUrl(url: String, metadata: UrlMetadata = UrlMetadata()) : VideoUrl(false, url, metadata) {
  override fun defaultFileFormat() = FileFormats.MP4
}
