package com.sheryv.util.unit

abstract class ScientificValue<V : Number, T : MetricUnit> {
  abstract val value: V
  abstract val unit: T
}
