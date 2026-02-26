package com.sheryv.tools.webcrawler.process.base.model.browserevent

data class BrowserResponseEvent(
  val frameId: String,
  val hasExtraInfo: Boolean,
  val loaderId: String? = null,
  val requestId: String? = null,
  val request: BrowserRequest? = null,
  val response: Response,
  val timestamp: Double,
  val type: String
) {
  fun toLine() = "$requestId [$type, ${response.mimeType}] ${response.url} [$frameId]"
}

data class BrowserRequest(
  val method: String,
  val url: String,
  val headers: Map<String, String> = emptyMap(),
  val initialPriority: String? = null,
  val isSameSite: Boolean? = null,
  val mixedContentType: String? = null,
  val referrerPolicy: String? = null,
)
