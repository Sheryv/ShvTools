package com.sheryv.tools.webcrawler.process.base.model

open class Step<T, R>(val name: String, private val runBlock: Process<T, R>, private val initialValue: T? = null) {
  suspend fun run(arg: T? = null):R? {
    return runBlock.process(arg)
  }
}

open class SimpleStep(name: String, runBlock: suspend () -> Unit) : Step<Any, Any>(name, { runBlock() }) {
}

fun interface Process<T, R> {
  suspend fun process(arg: T?): R?
}
