package com.sheryv.util.fx.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.internal.MainDispatcherFactory
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.newSingleThreadContext
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
class ExplicitMainDispatcherFactory2 : MainDispatcherFactory {
  // Higher load priority ensures your factory is chosen
  override val loadPriority: Int = 1000
  
  override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher {
    println("loading dispatchers")
    return  Dispatchers.JavaFx
  }
}

class ExplicitMainDispatcher : MainCoroutineDispatcher() {
  // A single thread to simulate the "Main" UI thread
  private val executor = newSingleThreadContext("CLI-Main")
  
  override val immediate: MainCoroutineDispatcher = this
  
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun dispatch(context: CoroutineContext, block: Runnable) {
    executor.dispatch(context, block)
  }
}


