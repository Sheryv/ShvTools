package com.sheryv.util

import com.sheryv.util.event.AsyncEvent
import com.sheryv.util.event.EventBus
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Tests {
  
  @Test
  fun name() {
    
    
      val o = "sad"
      EventBus.global.emit(E("1"))
      log.debug("subscribe")
    
    EventBus.global.subscribe<E>(o) {
      log.debug("on event E $it")
    }
    EventBus.global.subscribe<F>(o) {
      log.debug("on event F $it")
    }
    
    Thread.sleep(20)
    
      EventBus.global.emit(E("2"))
    runBlocking {
      EventBus.global.emit(E("3"))
      delay(1)
      
      EventBus.global.emitWait(E("4"))
      EventBus.global.emitWait(F("42"))
      EventBus.global.emitWait(E("43"))
      
      EventBus.global.emit(E("5"))
      delay(1)
      EventBus.global.unsubscribe(o)
      delay(1000)
      log.debug("unsubscribe")
    }
  }
  
  
  data class E(val payload: String) : AsyncEvent
  data class F(val payload: String) : AsyncEvent
}

