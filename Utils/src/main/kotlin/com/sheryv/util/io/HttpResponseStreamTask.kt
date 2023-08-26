package com.sheryv.util.io

import com.sheryv.util.unit.BinarySize
import com.sheryv.util.unit.BinaryTransferSpeed
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.FileOutputStream
import java.io.InputStream
import java.net.http.HttpResponse
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min

class HttpResponseStreamTask(
  private val response: HttpResponse<InputStream>,
  private val debounce: Long = 10
) : CoroutineScope {
  val flow = MutableSharedFlow<DataTransferProgress>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()
  
  private var totalSize: Long? = null
  private var avgSpeed: Double = 0.0
  private var currentBytes: Long = 0
  private var startTime: Long? = null
  
  fun toFile(output: Path, bufferSize: Int = 1024 * 64, onProgress: suspend (DataTransferProgress) -> Unit): DataTransferProgressSummary {
    init(onProgress)
    try {
      response.body().use { inputStream ->
        val readableByteChannel: ReadableByteChannel = Channels.newChannel(inputStream)
        
        FileOutputStream(output.toFile()).use {
          val channel: FileChannel = it.getChannel()
          var read = channel.transferFrom(readableByteChannel, 0, bufferSize.toLong()).toInt()
          var transferred = read
          while (read > 0) {
            onChunkProcessed(read)
            if (!isActive) {
              throw DataTransferCancelledException(currentBytes, response.uri().toString())
            }
            
            read = channel.transferFrom(readableByteChannel, transferred.toLong(), bufferSize.toLong()).toInt()
            transferred += read
          }
        }
      }
      return DataTransferProgressSummary(currentBytes, BinaryTransferSpeed.calc(avgSpeed), Instant.ofEpochMilli(startTime!!), Instant.now())
    } finally {
      cancel()
    }
  }
  
  
  fun toSeq(
    bufferSize: Int = 1024 * 8,
    onProgress: suspend (DataTransferProgress) -> Unit
  ): Pair<DataTransferProgressSummary, Sequence<ByteArray>> {
    init(onProgress)
    try {
      val seq = sequence {
        response.body().use { inputStream ->
          
          val buffer = ByteArray(bufferSize)
          
          var read = inputStream.read(buffer)
          var transferred = read
          while (read > 0) {
            if (read < buffer.size) {
              yield(buffer.copyInto(ByteArray(read), 0, 0, read))
            } else {
              yield(buffer)
            }
            
            onChunkProcessed(read)
            if (!isActive) {
              throw DataTransferCancelledException(currentBytes, response.uri().toString())
            }
            
            read = inputStream.read(buffer)
            transferred += read
          }
        }
      }
      return DataTransferProgressSummary(
        currentBytes,
        BinaryTransferSpeed.calc(avgSpeed),
        Instant.ofEpochMilli(startTime!!),
        Instant.now()
      ) to seq
    } finally {
      cancel()
    }
  }
  
  private fun onChunkProcessed(count: Int) {
    currentBytes += count
    avgSpeed = currentBytes / ((System.currentTimeMillis() - startTime!!) / 1000.0)
    val progress = DataTransferProgress(Instant.ofEpochMilli(startTime!!), totalSize, avgSpeed, currentBytes)
    flow.tryEmit(progress)
  }
  
  private fun init(onProgress: suspend (DataTransferProgress) -> Unit) {
    check(startTime == null) { "Task was already started at $startTime" }
    totalSize = response.headers().firstValue("Content-Length").map { it.toLong() }.filter { it > 0 }.getOrNull()
    flow.onEach(onProgress).launchIn(this)
    startTime = System.currentTimeMillis()
  }
}


class DataTransferProgressSummary(
  override val totalSizeBytes: Long,
  override val avgSpeed: BinaryTransferSpeed,
  startTime: Instant,
  val finishTime: Instant,
  override val totalSize: BinarySize = BinarySize.calc(totalSizeBytes),
) : DataTransferProgress(startTime, totalSizeBytes, avgSpeed.bytes.toDouble(), totalSizeBytes) {
  val duration = Duration.between(startTime, finishTime)
}

open class DataTransferProgress(
  open val startTime: Instant,
  open val totalSizeBytes: Long?,
  open val avgSpeedBytes: Double,
  open val currentSizeBytes: Long,
) {
  open val totalSize: BinarySize? by lazy { totalSizeBytes?.let { BinarySize.calc(it) } }
  
  open val currentSize: BinarySize by lazy { BinarySize.calc(currentSizeBytes) }
  
  open val currentRatio: Double? = totalSizeBytes?.let { currentSizeBytes.toDouble() / it }
  
  open val avgSpeed: BinaryTransferSpeed by lazy { BinaryTransferSpeed.calc(avgSpeedBytes) }
  
  fun formatRatioAsPercent(withDecimal: Boolean = true): String {
    if (currentRatio == null) {
      return if (withDecimal) "0.0" else "0"
    }
    
    val ratio = max(0.0, min(currentRatio!! * 100, 100.0))
    return if (withDecimal) String.format("%3.1f", ratio) else String.format("%3d", Math.round(ratio))
  }

//  fun sum(other: DownloadProgress): DownloadProgress {
//    val allSize = size + other.size
//    val progress = DownloadProgress(allSize)
//    val downloadedBytes: Long = currentBytes + other.currentBytes
//    progress.currentBytes = downloadedBytes
//    progress.currentSpeed = (currentSpeed + other.currentSpeed) / 2
//    progress.currentRatio = downloadedBytes.toDouble() / allSize
//    return progress
//  }
}

class DataTransferCancelledException(atByte: Long, url: String) :
  RuntimeException("Data transfer was cancelled prematurely, at byte $atByte for url $url")
