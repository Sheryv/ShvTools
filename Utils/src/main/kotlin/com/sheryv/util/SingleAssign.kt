package com.sheryv.util

import kotlin.reflect.KProperty

enum class SingleAssignThreadSafetyMode {
  SYNCHRONIZED,
  NONE
}

fun <T> singleAssign(threadSafetyMode: SingleAssignThreadSafetyMode = SingleAssignThreadSafetyMode.NONE): SingleAssign<T> =
  if (threadSafetyMode == SingleAssignThreadSafetyMode.SYNCHRONIZED) SynchronizedSingleAssign() else UnsynchronizedSingleAssign()

interface SingleAssign<T> {
  fun isInitialized(): Boolean
  operator fun getValue(thisRef: Any?, property: KProperty<*>): T
  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

private class SynchronizedSingleAssign<T> : UnsynchronizedSingleAssign<T>() {
  
  @Volatile
  override var _value: Any? = UNINITIALIZED_VALUE
  
  override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = synchronized(this) {
    super.setValue(thisRef, property, value)
  }
}

private open class UnsynchronizedSingleAssign<T> : SingleAssign<T> {
  
  protected object UNINITIALIZED_VALUE
  
  protected open var _value: Any? = UNINITIALIZED_VALUE
  
  override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    if (!isInitialized()) throw UninitializedPropertyAccessException("Value has not been assigned yet!")
    @Suppress("UNCHECKED_CAST")
    return _value as T
  }
  
  override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    if (isInitialized()) throw Exception("Value has already been assigned!")
    _value = value
  }
  
  override fun isInitialized() = _value != UNINITIALIZED_VALUE
}
