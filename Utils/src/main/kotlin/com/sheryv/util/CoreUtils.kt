@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(DelicateCoroutinesApi::class)

package com.sheryv.util

import com.sheryv.util.event.AsyncEvent
import com.sheryv.util.event.AsyncEventHandler
import com.sheryv.util.event.EventBus
import com.sheryv.util.logging.log
import kotlinx.coroutines.*

object CoreUtils {
//  val eventBus: EventBus = EventBus.builder()
//    .logNoSubscriberMessages(false)
//    .sendNoSubscriberEvent(false)
//    .installDefaultEventBus()
  
  
  fun parseBoolean(s: String?) = "1" == s || "true".equals(s, true) || "yes".equals(s, true) || "y".equals(s, true)
}

fun inBackground(
  toUpdate: EditableValue<Boolean>? = null,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): Job {
  toUpdate?.set(true)
  return GlobalScope.launch(Dispatchers.IO, start, {
    try {
      block()
    } catch (e: CancellationException) {
      log.trace("Coroutine canceled", e)
    }
  }).apply {
    invokeOnCompletion {
      if (it != null && it !is CancellationException) {
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

fun <T> inBackgroundAsync(
  toUpdate: EditableValue<Boolean>? = null,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> T
): Deferred<T> {
  toUpdate?.set(true)
  return GlobalScope.async(Dispatchers.IO, start, {
    try {
      return@async block()
    } catch (e: CancellationException) {
      log.trace("Coroutine canceled", e)
      throw e
    }
  }).apply {
    invokeOnCompletion {
      if (it != null && it !is CancellationException) {
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
  return GlobalScope.launch(Dispatchers.Main, start, {
    try {
      block()
    } catch (e: CancellationException) {
      log.trace("Coroutine canceled", e)
    }
  }).apply {
    invokeOnCompletion {
      if (it != null && it !is CancellationException) {
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

inline fun Any.subscribeEvents(noinline block: suspend (AsyncEvent) -> Unit): Any {
  EventBus.global.subscribe(this, AsyncEvent::class, block = block)
  log.debug("Registered all event handler to ${this.javaClass.simpleName}")
  return this
}

inline fun Any.unsubscribeAllEvents(): Any {
  if (EventBus.global.unsubscribe(this)) {
    log.debug("Unregistered all event handlers from ${this.javaClass.simpleName}")
  }
  return this
}


inline fun emitEvent(event: AsyncEvent) {
  EventBus.global.emit(event)
//  CoreUtils.eventBus.post(event)
}

suspend inline fun emitEventWait(event: AsyncEvent) {
  EventBus.global.emitWait(event)
}

