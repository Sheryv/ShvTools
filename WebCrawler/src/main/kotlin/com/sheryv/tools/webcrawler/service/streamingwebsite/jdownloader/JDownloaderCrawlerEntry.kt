package com.sheryv.tools.webcrawler.service.streamingwebsite.jdownloader

data class JDownloaderCrawlerEntry(
    val text: String,
    val filename: String,
    val downloadFolder: String,
    val packageName: String,
    val comment: String = "",
    val autoConfirm: String = "FALSE",
    val autoStart: String = "FALSE",
    val extractAfterDownload: String = "FALSE",
    val forcedStart: String = "FALSE",
    val overwritePackagizerEnabled: Boolean = false,
    val enabled: String = "TRUE",
)
