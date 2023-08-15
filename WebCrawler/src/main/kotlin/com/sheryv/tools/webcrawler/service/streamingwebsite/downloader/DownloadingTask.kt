package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

import com.sheryv.tools.webcrawler.service.streamingwebsite.downloader.DownloadingState.*
import com.sheryv.tools.webcrawler.utils.lg
import com.sheryv.util.DownloadProgress
import com.sheryv.util.FileSize
import com.sheryv.util.Strings
import com.sheryv.util.logging.log
import java.nio.file.Path

abstract class DownloadingTask(
  val output: Path,
  val url: String,
  val id: String = Strings.generateId(7),
  protected val config: DownloaderConfig,
) {
  
  var state: DownloadingState = QUEUED
    protected set
  
  open suspend fun startAndWait(onStateChange: (state: DownloadingState, (DownloadingState) -> Unit) -> Unit): Path? {
    if (state != PREPROCESS && state != QUEUED) {
      throw IllegalArgumentException("Downloading already started")
    }
    onStateChange(PREPROCESS, this::changeState)
    log.debug("Preparing download of ${fileName()} [$id]")
    
    preProcess()
    
    onStateChange(IN_PROGRESS, this::changeState)
    
    checkIfStopped()
    
    log.debug("Starting download of ${fileName()} [$id] approx. ${extrapolateSize().full()}")
    val startTime = System.currentTimeMillis()
    
    transfer()
    if (state == FAILED) {
      log.error("Failed download of ${fileName()} [$id]")
      return null
    }
    
    onStateChange(POST_PROCESS, this::changeState)
    
    checkIfStopped()
    
    val avgSpeed = avgSpeed(System.currentTimeMillis() - startTime)
    
    val file = postProcess()
    onStateChange(COMPLETED, this::changeState)
    
    checkIfStopped()
    log.debug("Finished download of ${fileName()} [$id] with avg. speed ${avgSpeed.sizeFormatted}${avgSpeed.unit}/s")
    
    return file
  }
  
  protected fun changeState(state: DownloadingState) {
    this.state = state
  }
  
  protected open suspend fun preProcess() {
  
  }
  
  
  protected open suspend fun transfer() {
  
  }
  
  protected open suspend fun postProcess(): Path {
    return output
  }
  
  protected fun checkIfStopped() {
    if (state == STOPPED) {
      throw ProcessStoppedException()
    }
  }
  
  abstract fun stop()
  
  abstract fun avgSpeed(durationMillis: Long): FileSize
  
  abstract fun extrapolateSize(): FileSize
  
  abstract fun progress(): DownloadProgress?
  
  fun fileName() = output.fileName.toString()

//  override fun compareTo(other: DownloadingTask): Int {
//    return (offset - other.offset).toInt()
//  }
  
  fun setStarted() {
    changeState(PREPROCESS)
  }
}
