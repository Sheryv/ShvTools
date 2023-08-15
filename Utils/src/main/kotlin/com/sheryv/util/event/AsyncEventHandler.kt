package com.sheryv.util.event

@FunctionalInterface
interface AsyncEventHandler {
  fun handleEvent(e: AsyncEvent)
}
