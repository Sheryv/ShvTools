package com.sheryv.util.event

import com.sheryv.util.logging.log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class EventBus : AutoCloseable {
  protected val eventFlow = MutableSharedFlow<AsyncEvent>(extraBufferCapacity = 100, onBufferOverflow = BufferOverflow.SUSPEND)
  
  //  protected val subscriptions = ConcurrentHashMap<Any, MutableList<Job>>()
  protected val subscriptions = ConcurrentHashMap<Any, MutableList<Handler<out AsyncEvent>>>()
  protected val job: Job
  
  init {
    job = eventFlow
      .onEach { e ->
        subscriptions.asSequence()
          .flatMap { it.value }
          .filter { it.eventType == AsyncEvent::class || it.eventType.isInstance(e) }
          .forEach {
            try {
              (it as Handler<AsyncEvent>).block(e)
            } catch (e: Exception) {
              log.error("Error executing event handler for ${it.eventType.simpleName}", e)
            }
          }
      }
      .launchIn(GlobalScope)
  }
  
  fun <T : AsyncEvent> subscribe(owner: Any, type: KClass<T>, block: suspend (T) -> Unit) {
    subscriptions.compute(owner) { k, v ->
      v?.apply { add(Handler(type, block)) } ?: mutableListOf(Handler(type, block))
    }
  }
  
  inline fun <reified T : AsyncEvent> subscribe(owner: Any, noinline block: suspend (T) -> Unit) {
    subscribe(owner, T::class, block)
  }
  
  fun unsubscribe(owner: Any): Boolean {
    return subscriptions.remove(owner) != null
  }
  
  suspend fun emitWait(event: AsyncEvent) {
    eventFlow.emit(event)
  }
  
  fun emit(event: AsyncEvent) {
    val done = eventFlow.tryEmit(event)
    if (!done) {
      log.debug("Event dropped {}", event.javaClass.simpleName)
    }
  }
  
  fun registeredSubsctiptionsCount(owner: Any) = subscriptions[owner]?.size ?: 0
  
  override fun close() {
    job.cancel()
  }
  
  companion object {
    val global = EventBus()
  }
  
  protected data class Handler<T : AsyncEvent>(val eventType: KClass<T>, val block: suspend (T) -> Unit)
}


