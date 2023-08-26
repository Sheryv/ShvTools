package com.sheryv.util.unit

class RateUnit<T : MetricUnit>(val base: T, override val label: String = base.label + "/sec.") : MetricUnit {
  override fun toString(): String = "$base/s"
}
