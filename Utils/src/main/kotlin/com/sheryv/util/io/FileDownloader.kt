package com.sheryv.util.io

import com.sheryv.util.inBackground
import java.nio.file.Path

class FileDownloader(
  val url: String,
  val outputPath: Path,
  private val onProgress: suspend (DataTransferProgress) -> Unit = {}
) {
  
  var progress: DataTransferProgress? = null
    private set
  
  var completeProgress: DataTransferProgressSummary? = null
    private set
  
  var isComplete = false
    private set
  var isSuccessful = false
    private set
  var started = false
    private set
  
  suspend fun downloadAsync(): FileDownloader {
    inBackground { downloadSync() }
    return this
  }
  
  fun downloadSync(): DataTransferProgressSummary {
    check(!started) { "Cannot start running download" }
    try {
      started = true
      completeProgress = HttpSupport().downloadWithProgress(HttpSupport.getRequest(url), outputPath) {
        progress = it
        onProgress(it)
      }
      progress = completeProgress
      isSuccessful = true
      return completeProgress!!
    } finally {
      isComplete = true
    }
  }
}
