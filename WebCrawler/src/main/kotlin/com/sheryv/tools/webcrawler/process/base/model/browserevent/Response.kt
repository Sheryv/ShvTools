package com.sheryv.tools.webcrawler.process.base.model.browserevent

data class Response(
  val connectionId: Int? = null,
  val connectionReused: Boolean? = null,
  val encodedDataLength: Int? = null,
  val fromDiskCache: Boolean? = null,
  val fromPrefetchCache: Boolean? = null,
  val fromServiceWorker: Boolean? = null,
  val headers: Map<String, String>? = null,
  val mimeType: String,
  val protocol: String? = null,
  val remoteIPAddress: String? = null,
  val remotePort: Int? = null,
  val responseTime: Double? = null,
  val securityState: String? = null,
  val status: Int,
  val statusText: String,
  val url: String
)
