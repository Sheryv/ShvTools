package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

enum class DownloadingState(val label: String, val isStarted: Boolean, val isCompleted: Boolean) {
  QUEUED("Queued", false, false),
  STOPPED("Stopped", false, false),
  PREPROCESS("Preparing", true, false),
  IN_PROGRESS("Transferring", true, false),
  POST_PROCESS("Post-processing", true, false),
  COMPLETED("Completed", true, true),
  FAILED("Failed", true, true),
  ;
  
  fun inBeingProcessed() = isStarted && !isCompleted
  
  fun hasStats() = (isStarted && this != PREPROCESS) || this == STOPPED
}
