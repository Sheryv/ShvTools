package com.sheryv.util.unit

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

abstract class BinarySizeBase<T : MetricUnit>() : ScientificValue<Double, T>() {
  abstract val bytes: Long
  
  protected abstract val formatter: DecimalFormat
  
  val formattedWithoutUnit: String by lazy { formatter.format(value) }
  val formatted: String by lazy { "$formattedWithoutUnit $unit" }
  
  init {
    require(value >= 0) { "Incorrect size $value" }
  }
  
  companion object {
    @JvmStatic
    val defaultFormatter = DecimalFormat("# ##0.#")
    
    @JvmStatic
    protected fun recalculate(value: Double, unit: BinaryUnit): Triple<Double, Long, BinaryUnit> {
      return if (value == 0.0) {
        Triple(0.0, 0, BinaryUnit.B)
      } else {
        val bytes = (1024.0.pow(unit.scale) * value).toLong()
        
        val scale = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val res = bytes / 1024.0.pow(scale.toDouble())
        val u = BinaryUnit.entries.firstOrNull { it.scale == scale } ?: BinaryUnit.PB
        
        Triple(res, bytes, u)
      }
    }
  }
}
