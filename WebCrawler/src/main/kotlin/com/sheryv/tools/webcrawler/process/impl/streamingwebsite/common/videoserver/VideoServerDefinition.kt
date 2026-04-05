package com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.videoserver

import com.sheryv.tools.webcrawler.process.base.SeleniumCrawler
import com.sheryv.tools.webcrawler.process.impl.streamingwebsite.common.StreamingWebsiteBase

open class VideoServerDefinition(
  val id: String,
  val label: String,
  val domains: List<String>,
  val isHLS: Boolean,
  val searchTerm: Regex,
  private val builder: (VideoServerDefinition, StreamingWebsiteBase) -> VideoServerHandler<*> = ::VideoServerHandler
) {
  
  constructor(
    id: String,
    label: String,
    domains: List<String>,
    isHLS: Boolean,
    builder: (VideoServerDefinition, StreamingWebsiteBase) -> VideoServerHandler<*> = ::VideoServerHandler
  ) : this(id, label, domains, isHLS, Regex(Regex.escape(label.lowercase())), builder)
  
  open fun buildHandler(crawler: StreamingWebsiteBase): VideoServerHandler<*> {
    return builder.invoke(this, crawler)
  }
  
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VideoServerDefinition) return false
    
    if (id != other.id) return false
    
    return true
  }
  
  override fun hashCode(): Int {
    return id.hashCode()
  }
}


//typealias HandlerBuilder<H> = (
//  def: VideoServerDefinition,
//  scraper: SeleniumCrawler<*>,
//  innerIframeCssSelector: By?,
//  overrideFileFormat: FileFormats?,
//  scriptToActivatePlayer: String?,
//  scriptToCheckPlayerReady: String?,
//) -> H

//open class PreBuiltServerDefinition(
//  id: String,
//  label: String,
//  searchTerm: Regex,
//  domains: List<String>,
//  protected val builder: HandlerBuilder<VideoServerHandler<*>>
//) : VideoServerDefinition(id, label, searchTerm, domains, true) {
//
//  override fun buildHandler(crawler: SeleniumCrawler<*>): VideoServerHandler<*> {
//   return builder.invoke(this, crawler, )
//  }
//}
