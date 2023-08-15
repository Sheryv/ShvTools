package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

import java.nio.file.Path

data class DownloaderConfig(
  val tempDirPath: Path = Path.of(System.getProperty("java.io.tmpdir")).resolve("webcrawler"),
  val concurrentDownloads: Int = 2,
  val connectionsPerFile: Int = 5,
  val maxRetries: Int = 5,
  val defaultDownloadDir: Path = Path.of(System.getProperty("user.home"), "Downloads")
) {
}
