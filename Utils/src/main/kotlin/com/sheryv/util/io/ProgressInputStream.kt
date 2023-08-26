package com.sheryv.util.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.InputStream
import java.net.http.HttpResponse
import kotlin.coroutines.CoroutineContext


class ProgressInputStream(
  private val input: InputStream,
  private val response: HttpResponse<InputStream>,
  onProgress: suspend (ProgressPart) -> Unit
) : InputStream(), CoroutineScope {
  val flow = MutableSharedFlow<ProgressPart>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  
  override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()
  
  private var readCount = 0L
  
  init {
    flow.debounce(10).onEach(onProgress).launchIn(this)
  }
  
  override fun read(b: ByteArray): Int {
    val readCount = input.read(b)
    notify(readCount.toLong(), b)
    return readCount
  }
  
  override fun read(b: ByteArray, off: Int, len: Int): Int {
    val readCount = input.read(b, off, len)
    notify(readCount.toLong(), b)
    return readCount
  }
  
  override fun skip(n: Long): Long {
    val skip = input.skip(n)
    notify(skip, byteArrayOf())
    return skip
  }
  
  override fun read(): Int {
    val read = input.read()
    if (read != -1) {
      notify(1, byteArrayOf(read.toByte()))
    }
    return read
  }
  
  override fun close() {
    super.close()
    cancel()
  }
  
  private fun notify(count: Long, part: ByteArray) {
    if (count != -1L) {
      this.readCount += count
      flow.tryEmit(ProgressPart(readCount, part, response))
    }
  }
  
  class ProgressPart(val allReadCount: Long, val part: ByteArray, val response: HttpResponse<InputStream>)
}
