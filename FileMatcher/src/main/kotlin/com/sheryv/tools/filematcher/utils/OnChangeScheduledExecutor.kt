package com.sheryv.tools.lasso.util

import com.sheryv.tools.filematcher.utils.inBackground
import com.sheryv.tools.filematcher.utils.lg
import kotlinx.coroutines.*

class OnChangeScheduledExecutor(private val name: String, private val delayMillis: Long = 400, private val onChange: suspend () -> Unit) {
  private var refreshJob: Job? = null
  private var changed: Boolean = false
  
  
  fun start(delayMillis: Long = this.delayMillis): Job {
    lg(javaClass.name + "#" + name).info("Refresh coroutine [$name] started with rate $delayMillis")
    refreshJob = inBackground {
      while (refreshJob?.isActive == true) {
        if (changed) {
          changed = false
          lg(javaClass.name + "#" + name).debug("Refresh coroutine [$name] doing refresh")
          onChange()
        }
        delay(delayMillis)
      }
    }
    
    refreshJob!!.start()
    return refreshJob!!
  }
  
  fun stop() {
    refreshJob?.cancel()
    lg(javaClass.name + "#" + name).info("Refresh coroutine [$name] stopped")
  }
  
  fun executeNow() {
    changed = false
    inBackground {
      onChange()
    }
  }
  
  fun markChanged() {
    changed = true
  }
}
