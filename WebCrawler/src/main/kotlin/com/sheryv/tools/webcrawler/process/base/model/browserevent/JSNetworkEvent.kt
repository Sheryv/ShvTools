package com.sheryv.tools.webcrawler.process.base.model.browserevent

data class JSNetworkEvent(
    val connectEnd: Int,
    val connectStart: Int,
    val domainLookupEnd: Int,
    val domainLookupStart: Int,
    val duration: Double,
    val entryType: String,
    val fetchStart: Double,
    val initiatorType: String,
    val name: String,
    val nextHopProtocol: String,
    val redirectEnd: Int,
    val redirectStart: Int,
    val requestStart: Int,
    val responseEnd: Int,
    val responseStart: Int,
    val startTime: Double,
    val transferSize: Int,
    val workerStart: Int
)
