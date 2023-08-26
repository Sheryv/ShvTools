package com.sheryv.tools.webcrawler.service.streamingwebsite.downloader

import com.sheryv.tools.webcrawler.service.streamingwebsite.downloader.DownloadingState.*
import com.sheryv.util.Strings
import com.sheryv.util.io.DataTransferProgress
import com.sheryv.util.logging.log
import com.sheryv.util.unit.BinaryTransferSpeed
import java.nio.file.Path
import java.time.Instant

abstract class DownloadingTask(
  val output: Path,
  val url: String,
  val id: String = Strings.generateId(7),
  protected val config: DownloaderConfig,
) {
  
  protected var initTime: Instant? = null
  protected var startTime: Instant? = null
  protected var finishTime: Instant? = null
  
  var state: DownloadingState = QUEUED
    protected set
  
  open suspend fun startAndWait(onStateChange: (state: DownloadingState, (DownloadingState) -> Unit) -> Unit): Path? {
    if (state != PREPROCESS && state != QUEUED) {
      throw IllegalArgumentException("Downloading already started")
    }
    initTime = Instant.now()
    onStateChange(PREPROCESS, this::changeState)
    log.debug("Preparing download of ${fileName()} [$id]")
    
    preProcess()
    
    onStateChange(IN_PROGRESS, this::changeState)
    
    checkIfStopped()
    
    log.debug("Starting download of ${fileName()} [$id]")
    startTime = Instant.now()
    
    transfer()
    if (state == FAILED) {
      log.error("Failed download of ${fileName()} [$id]")
      return null
    }
    
    onStateChange(POST_PROCESS, this::changeState)
    
    checkIfStopped()
    
    val avgSpeed = avgSpeed(System.currentTimeMillis() - startTime!!.toEpochMilli())
    
    val file = postProcess()
    onStateChange(COMPLETED, this::changeState)
    
    checkIfStopped()
    log.debug("Finished download of ${fileName()} [$id] with avg. speed ${avgSpeed.formatted}")
    finishTime = Instant.now()
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
  
  abstract fun avgSpeed(durationMillis: Long): BinaryTransferSpeed
  
  abstract fun extrapolateSize(): Long
  
  abstract fun progress(): DataTransferProgress?
  
  abstract fun currentlyDownloadedBytes(): Long
  
  fun fileName() = output.fileName.toString()

//  override fun compareTo(other: DownloadingTask): Int {
//    return (offset - other.offset).toInt()
//  }
  
  fun setStarted() {
    changeState(PREPROCESS)
  }
}
