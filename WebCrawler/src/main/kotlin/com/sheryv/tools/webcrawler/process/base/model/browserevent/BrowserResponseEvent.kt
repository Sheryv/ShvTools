package com.sheryv.tools.webcrawler.process.base.model.browserevent

data class BrowserResponseEvent(
  val frameId: String,
  val hasExtraInfo: Boolean,
  val loaderId: String? = null,
  val requestId: String? = null,
  val response: Response,
  val timestamp: Double,
  val type: String
)
