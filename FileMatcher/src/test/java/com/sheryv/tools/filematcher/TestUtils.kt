package com.sheryv.tools.filematcher

import kotlin.system.measureNanoTime

object TestUtils {
  
  inline fun <reified R> measureLog(noinline block: () -> R): R {
    val res: R;
    val time = measureNanoTime {
      res = block()
    } / 1_000_000.0
    println("Calculated time: $time ms")
    return res
  }
  
  inline fun <reified R> measure(noinline block: () -> R): Double {
    return measureNanoTime {
      block()
    } / 1_000_000.0
  }
}
