package com.sheryv.util

import java.util.*

class Observable<T>(initial: T? = null) {
  private val listeners = Collections.synchronizedList(ArrayList<ChangeListener<T?>>())
  
  
  fun addListener(listener: ChangeListener<T?>) {
    listeners.add(listener)
  }
  
  @Synchronized
  fun removeListener(listener: ChangeListener<T?>) {
    try {
      listeners.remove(listener)
    } catch (e: Exception) {
      return
    }
  }
  
  var value: T? = initial
    set(value) {
      val oldValue: T? = field
      field = value
      firePropertyChange(oldValue, value)
    }
  
  
  fun firePropertyChange(oldValue: T?, newValue: T?) {
    var copy: List<ChangeListener<T?>>? = null
    var run = false
    synchronized(this) {
      if (oldValue == null || newValue == null || oldValue != newValue) {
        copy = ArrayList(listeners)
        run = true
      }
    }
    if (run) {
      for (changeListener in copy!!) {
        changeListener.onChange(oldValue, newValue)
      }
    }
  }
}
