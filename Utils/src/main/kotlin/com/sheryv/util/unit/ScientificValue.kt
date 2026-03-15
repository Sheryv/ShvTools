package com.sheryv.util.unit

abstract class ScientificValue<V : Number, T : MetricUnit> {
  abstract val value: V
  abstract val unit: T
  
  
  override fun toString(): String {
    return String.format(if(value is Double || value is Float) "%.2f %s" else "%d %s", value, unit)
  }
}
