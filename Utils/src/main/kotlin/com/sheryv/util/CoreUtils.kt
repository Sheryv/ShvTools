@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(DelicateCoroutinesApi::class)

package com.sheryv.util

import com.sheryv.util.event.AsyncEvent
import com.sheryv.util.event.AsyncEventHandler
import com.sheryv.util.event.EventBus
import com.sheryv.util.event.EventBus.Companion.global
import com.sheryv.util.logging.log
import kotlinx.coroutines.*

object CoreUtils {
//  val eventBus: EventBus = EventBus.builder()
//    .logNoSubscriberMessages(false)
//    .sendNoSubscriberEvent(false)
//    .installDefaultEventBus()
  
  
  suspend fun wait(intervalMs: Long = 100, timeoutMs: Long = 2000, check: suspend (Int) -> Boolean) {
    waitFor(intervalMs, timeoutMs) {
      check(it) to null
    }
  }
  
  suspend fun <T> waitForNotNull(intervalMs: Long = 100, timeoutMs: Long = 2000, check: suspend (Int) -> T?): T? {
    return waitFor(intervalMs, timeoutMs) {
      val result = check(it)
      (result != null) to result
    }
  }
  
  suspend fun <T> waitFor(intervalMs: Long = 100, timeoutMs: Long = 2000, check: suspend (Int) -> Pair<Boolean, T?>): T? {
    var result: T? = null
    var correct = false
    var index = 0
    var start = System.currentTimeMillis()
    while (!correct && System.currentTimeMillis() < start + timeoutMs) {
      val r = check(index++)
      correct = r.first
      result = r.second
      delay(intervalMs)
    }
    return result
  }
  
  
  fun parseBoolean(s: String?) = "1" == s || "true".equals(s, true) || "yes".equals(s, true) || "y".equals(s, true)
}

fun inBackground(
  toUpdate: EditableValue<Boolean>? = null,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): Job {
  toUpdate?.set(true)
  return GlobalScope.launch(Dispatchers.IO, start, block).apply {
    invokeOnCompletion {
      if (it != null && it is CancellationException) {
        log.trace("Coroutine canceled", it)
      } else if (it != null) {
        log.error("Error executing async block", it)
      }
      if (toUpdate != null) {
        inMainThread {
          toUpdate.set(false)
        }
      }
    }
  }
}

fun <T> inBackgroundWithResult(
  toUpdate: EditableValue<Boolean>? = null,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> T
): Deferred<T> {
  toUpdate?.set(true)
  return GlobalScope.async(Dispatchers.IO, start, block).apply {
    invokeOnCompletion {
      if (it != null && it is CancellationException) {
        log.trace("Coroutine canceled", it)
      } else if (it != null) {
        log.error("Error executing async block", it)
      }
      if (toUpdate != null) {
        inMainThread {
          toUpdate.set(false)
        }
      }
    }
  }
}

fun inMainThread(
  toUpdate: EditableValue<Boolean>? = null,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): Job {
  toUpdate?.set(true)
  return GlobalScope.launch(Dispatchers.Main, start, block).apply {
    invokeOnCompletion {
      if (it != null && it is CancellationException) {
        log.trace("Coroutine canceled", it)
      } else if (it != null) {
        log.error("Error executing async block", it)
      }
      toUpdate?.set(false)
    }
  }
//  return GlobalScope.launch(Dispatchers.Main, start) {
//    try {
//      block()
//    } catch (ignored: CancellationException) {
//    } catch (e: Exception) {
//      log.error("Error executing async block", e)
//    } finally {
//      toUpdate?.set(false)
//    }
//  }
}

suspend fun <T> inMainContext(
  toUpdate: EditableValue<Boolean>? = null,
  block: suspend CoroutineScope.() -> T
): T {
  toUpdate?.set(true)
  return withContext(Dispatchers.Main) {
    try {
      return@withContext block()
    } catch (e: Exception) {
      log.error("Error executing async block", e)
      throw e
    } finally {
      toUpdate?.set(false)
    }
  }
}

//inline fun eventsAttach(receiver: Any) {
//  CoreUtils.eventBus.register(receiver)
//  log.debug("Registered event bus to ${receiver::class.simpleName}")
//}
//
//inline fun eventsDetach(receiver: Any) {
//  if (CoreUtils.eventBus.isRegistered(receiver)) {
//    CoreUtils.eventBus.unregister(receiver)
//    log.debug("Unregistered event bus from ${receiver::class.simpleName}")
//  } else {
//    log.trace("Trying to unregister event listener that was not registered")
//  }
//}

/**
 * If true return first value, otherwise second
 */
inline fun <reified T> Boolean.ie(ifTrue: T, ifFalse: T): T {
  return if (this) ifTrue else ifFalse
}

inline fun <reified T : AsyncEvent> Any.subscribeEvent(noinline block: suspend (T) -> Unit): Any {
  EventBus.global.subscribe(this, T::class, block = block)
  log.debug("Registered event handler of ${T::class.simpleName} to ${this.javaClass.simpleName}")
  return this
}

inline fun AsyncEventHandler.subscribeEvents(): AsyncEventHandler {
  EventBus.global.subscribe(this, AsyncEvent::class, block = this::handleEvent)
  log.debug("Registered all event handler to ${this.javaClass.simpleName}")
  return this
}

inline fun Any.unsubscribeAllEvents(): Any {
  if (EventBus.global.unsubscribe(this)) {
    log.debug("Unregistered all event handlers from ${this.javaClass.simpleName}")
  }
  return this
}
