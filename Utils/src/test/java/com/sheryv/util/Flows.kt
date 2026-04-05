package com.sheryv.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

object Flows {
  val flow: Channel<String> = Channel<String>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  
  
  @JvmStatic
  fun main(args: Array<String>) {
    runBlocking {
      Executors.newSingleThreadExecutor().execute {
        for (s in 0..60) {
          println("Sending $s")
          flow.trySend("ITEM $s")
          Thread.sleep(250)
        }
      }
      
      delay(1000)
      var def = setupListenerAndWaitForCorrectEvent3 { it.contains("26") }
      println("listener ready")
      
      var res = def.await()
      println("Done first $res")
      
      delay(1000)
      def = setupListenerAndWaitForCorrectEvent3 { it.contains("36") }
      println("listener ready")
      
      res = def.await()
      println("Done sec $res")
    }
  }
  
  
  suspend fun setupListenerAndWaitForCorrectEvent3(
    filter: (data: String) -> Boolean
  ): Deferred<String> = inBackgroundWithResult {
    return@inBackgroundWithResult flow.receiveAsFlow().onEach { println("Processing $it") }.first { filter(it) }
  }
}
