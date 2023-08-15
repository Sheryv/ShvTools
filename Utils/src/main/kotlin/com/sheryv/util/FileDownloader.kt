package com.sheryv.util

import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Path
import java.time.Instant

private const val BUFFER = 1024 * 1024L

class FileDownloader(
  val url: String,
  val outputPath: Path
) {
  
  var progress: DownloadProgress? = null
    private set
  var isComplete = false
    private set
  var isSuccessful = false
    private set
  var started = false
    private set
  
  fun downloadAsync(): FileDownloader {
    Thread { downloadSync() }.start()
    return this
  }
  
  fun downloadSync(): Long {
    check(!started) { "Cannot start running download" }
    var httpConnection: HttpURLConnection? = null
    try {
      FileOutputStream(outputPath.toFile()).use { fileOutputStream ->
        var time = System.currentTimeMillis()
        val wrappedUrl = URL(url)
        httpConnection = wrappedUrl.openConnection() as HttpURLConnection
        val headerSize = httpConnection!!.contentLengthLong
        httpConnection!!.setConnectTimeout(30000)
        httpConnection!!.setReadTimeout(30000)
        progress = DownloadProgress(headerSize)
        started = true
        httpConnection!!.inputStream.use { inputStream ->
          val readableByteChannel: ReadableByteChannel = Channels.newChannel(inputStream)
          val channel: FileChannel = fileOutputStream.getChannel()
          var read: Long = channel.transferFrom(readableByteChannel, 0, BUFFER)
          while (read > 0) {
            val speed = read / ((System.currentTimeMillis() - time).toDouble() / 1000.0)
            if (speed >= 0) {
              progress!!.currentSpeed = speed
            }

//          System.out.printf("Done %-2.1f%% Speed %.3f KB/s %n", (double) size / headerSize * 100, speed / 1024);
            if (headerSize > 0) {
              progress!!.currentRatio = progress!!.currentBytes.toDouble() / headerSize
            }
            progress!!.increaseBytes(read)
            time = System.currentTimeMillis()
            read = channel.transferFrom(readableByteChannel, progress!!.currentBytes, BUFFER)
          }
          if (headerSize != progress!!.currentBytes) {
            System.out.printf("Sizes are different: %d, %d%n", progress!!.currentBytes, headerSize)
          }
          if (progress!!.currentBytes < 10) {
            progress!!.currentRatio = 0.0
            throw IllegalStateException("Empty stream returned")
          }
          isSuccessful = true
          progress!!.currentRatio = 1.0
          progress!!.finishTime = Instant.now()
          return progress!!.currentBytes
        }
      }
    } catch (e: IOException) {
      throw RuntimeException(e)
    } finally {
      isComplete = true
      if (httpConnection != null) {
        httpConnection!!.disconnect()
      }
    }
  }
}
