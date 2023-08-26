package com.sheryv.tools.webcrawler.view

import com.sheryv.util.inBackground
import com.sheryv.util.logging.log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class OnChangeScheduledExecutor(private val name: String, private val delayMillis: Long = 400, private val onChange: suspend () -> Unit) {
  private var refreshJob: Job? = null
  private var changed: Boolean = false
  
  
  fun start(delayMillis: Long = this.delayMillis): Job {
    log.trace("Refresh coroutine [$name] started with rate $delayMillis")
    refreshJob = inBackground {
      while (isActive) {
        if (changed) {
          changed = false
          log.trace("Refresh coroutine [$name] doing refresh")
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
    log.trace("Refresh coroutine [$name] stopped")
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
