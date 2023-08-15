package com.sheryv.util

import java.util.*

fun interface ChangeListener<T> : EventListener {
  fun onChange(oldValue: T, newValue: T)
}
