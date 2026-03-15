package com.sheryv.tools.webcrawler.view

import com.sheryv.util.inBackground
import com.sheryv.util.logging.log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

class OnChangeScheduledExecutor(private val name: String, private val delayMillis: Long = 400, private val onChange: suspend () -> Unit) {
  private var refreshJob: Job? = null
  private val changed: AtomicBoolean = AtomicBoolean(false)
  
  
  fun start(delayMillis: Long = this.delayMillis): Job {
    log.trace("Refresh coroutine [$name] started with rate $delayMillis")
    refreshJob = inBackground {
      while (isActive) {
        if (changed.compareAndExchange(true, false)) {
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
    changed.set(false)
    inBackground {
      onChange()
    }
  }
  
  fun markChanged() {
    changed.set(true)
  }
}
