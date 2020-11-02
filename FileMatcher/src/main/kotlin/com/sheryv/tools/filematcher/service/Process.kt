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

abstract class Process(val onFinish: ((ProcessResult<out Process>) -> Unit)? = null, private val attachEvents: Boolean = false) {
  private var job: Job? = null
  
  fun start() {
    if (attachEvents)
      eventsAttach(this)
    if (preValidation()) {
      job = inBackground {
        try {
          process()
          onEndAsync(ProcessResult(ResultType.SUCCESS, this@Process))
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
  
  protected suspend fun onEndAsync(res: ProcessResult<out Process>) {
    withContext(Dispatchers.Main) {
      onEnd(res)
    }
  }
  
  protected fun onEnd(res: ProcessResult<out Process>) {
    onFinish?.invoke(res)
    if (attachEvents)
      eventsDetach(this)
  }
  
  protected abstract suspend fun process()
  protected open fun preValidation() = true
  
  
  protected fun isActive(): Boolean {
    return job?.isActive ?: false
  }
}