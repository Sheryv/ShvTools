package com.sheryv.util.unit

import kotlin.math.log10
import kotlin.math.pow

class BitTransferSpeed(
  override val value: Double,
  override val unit: RateUnit<BitUnit>,
) : ScientificValue<Double, RateUnit<BitUnit>>() {
  val bits: Long = (1000.0.pow(unit.base.scale) * value).toLong()
  
  private constructor(bitsPerSecond: Long) : this(bitsPerSecond.toDouble(), zero.unit)
  
  val formatted: String by lazy { "${BinarySizeBase.defaultFormatter.format(value)} $unit" }
  
  companion object {
    @JvmStatic
    val zero = BitTransferSpeed(0.0, RateUnit(BitUnit.b))
    
    fun calc(value: Number, unit: BitUnit = BitUnit.b): BitTransferSpeed {
      val (r, u) = recalculate(value.toDouble(), unit)
      return BitTransferSpeed(r, RateUnit(u))
    }
    
    private fun recalculate(value: Double, unit: BitUnit): Pair<Double, BitUnit> {
      return if (value == 0.0) {
        Pair(0.0, BitUnit.b)
      } else {
        val bytes = (1000.0.pow(unit.scale) * value).toLong()
        
        val scale = (log10(bytes.toDouble()) / log10(1000.0)).toInt() - 1
        val res = bytes / 1000.0.pow(scale.toDouble())
        val u = BitUnit.entries.firstOrNull { it.scale == scale } ?: BitUnit.Pb
        
        Pair(res, u)
      }
    }
  }
  
  operator fun plus(other: BitTransferSpeed): BitTransferSpeed {
    return plus(other.bits)
  }
  
  operator fun plus(bytesPerSecond: Long): BitTransferSpeed {
    return BitTransferSpeed(this.bits + bytesPerSecond)
  }
  
  operator fun minus(other: BitTransferSpeed): BitTransferSpeed {
    return minus(other.bits)
  }
  
  operator fun minus(bytesPerSecond: Long): BitTransferSpeed {
    return BitTransferSpeed(this.bits - bytesPerSecond)
  }
  
  
}
