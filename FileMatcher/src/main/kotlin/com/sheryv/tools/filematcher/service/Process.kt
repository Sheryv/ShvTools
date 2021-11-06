package com.sheryv.tools.filematcher.service

import com.sheryv.tools.filematcher.model.ProcessResult
import com.sheryv.tools.filematcher.model.ResultType
import com.sheryv.tools.filematcher.utils.eventsAttach
import com.sheryv.tools.filematcher.utils.eventsDetach
import com.sheryv.tools.filematcher.utils.inBackground
import com.sheryv.tools.filematcher.utils.lg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

abstract class Process<Output>(
  val onFinish: ((ProcessResult<Output, out Process<Output>>) -> Unit)? = null,
  private val attachEvents: Boolean = false
) {
  private var job: Job? = null
  
  fun start() {
    if (attachEvents)
      eventsAttach(this)
    if (preValidation()) {
      job = inBackground {
        try {
          val output = process()
          onEndAsync(ProcessResult(ResultType.SUCCESS, this@Process, data = output))
        } catch (e: Exception) {
          lg().error("Error in process", e)
          onEndAsync(ProcessResult(ResultType.ERROR, this@Process, e))
        }
      }
    } else {
      onEnd(ProcessResult(ResultType.ERROR, this))
    }
  }
  
  fun stop() {
    job?.cancel()
    onEnd(ProcessResult(ResultType.ABORT, this))
  }
  
  protected suspend fun onEndAsync(res: ProcessResult<Output, out Process<Output>>) {
    withContext(Dispatchers.Main) {
      onEnd(res)
    }
  }
  
  protected fun onEnd(res: ProcessResult<Output, out Process<Output>>) {
    onFinish?.invoke(res)
    if (attachEvents)
      eventsDetach(this)
  }
  
  protected abstract suspend fun process(): Output
  protected open fun preValidation() = true
  
  
  protected fun isActive(): Boolean {
    return job?.isActive ?: false
  }
}
