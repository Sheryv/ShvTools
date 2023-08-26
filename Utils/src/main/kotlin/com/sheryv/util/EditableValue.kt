package com.sheryv.util

interface EditableValue<T> {
  fun get(): T
  fun set(value: T)
}
