package com.sheryv.util

import java.time.Instant
import kotlin.math.max
import kotlin.math.min

class DownloadProgress(val size: Long, val startTime: Instant = Instant.now()) {
  val sizeFromHeader: FileSize = FileSize(size)
  
  var currentSpeed = 0.0
  
  var currentRatio = 0.0
  
  var currentBytes: Long = 0
  
  var finishTime: Instant? = null
  
  fun increaseBytes(bytes: Long) {
    currentBytes += bytes
  }
  
  fun formatDownloaded(): String {
    return sizeFromHeader.reformatOther(currentBytes).full()
  }
  
  fun formatRatioAsPercent(withDecimal: Boolean): String {
    val ratio = max(0.0, min(currentRatio * 100, 100.0))
    return if (withDecimal) String.format("%3.1f", ratio) else String.format("%3d", Math.round(ratio))
  }
  
  fun formatSpeed(): String {
    val format: FileSize
    format = if (finishTime != null) {
      FileSize((currentBytes.toDouble() / (finishTime!!.epochSecond - startTime.epochSecond)).toLong())
    } else {
      sizeFromHeader.reformatOther(currentSpeed.toLong())
    }
    return java.lang.String.format("%s%s/s", format.sizeFormatted, format.unit)
  }
  
  fun sum(other: DownloadProgress): DownloadProgress {
    val allSize = size + other.size
    val progress = DownloadProgress(allSize)
    val downloadedBytes: Long = currentBytes + other.currentBytes
    progress.currentBytes = downloadedBytes
    progress.currentSpeed = (currentSpeed + other.currentSpeed) / 2
    progress.currentRatio = downloadedBytes.toDouble() / allSize
    return progress
  }
}
