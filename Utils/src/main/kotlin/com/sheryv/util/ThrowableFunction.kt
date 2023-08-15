package com.sheryv.util

fun interface ThrowableFunction<T, R> {
  @Throws(Exception::class)
  fun apply(t: T): R
}
