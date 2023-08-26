package com.sheryv.util.unit

import java.text.DecimalFormat
import kotlin.math.pow

class BinaryTransferSpeed(
  override val value: Double,
  override val unit: RateUnit<BinaryUnit>,
  override val formatter: DecimalFormat = defaultFormatter
) : BinarySizeBase<RateUnit<BinaryUnit>>() {
  override val bytes: Long = (1024.0.pow(unit.base.scale) * value).toLong()
  
  private constructor(bytesPerSecond: Long) : this(bytesPerSecond.toDouble(), zero.unit)
  
  companion object {
    @JvmStatic
    val zero = BinaryTransferSpeed(0.0, RateUnit(BinaryUnit.B))
    
    fun calc(value: Number, unit: BinaryUnit = BinaryUnit.B): BinaryTransferSpeed {
      val (r, b, u) = recalculate(value.toDouble(), unit)
      return BinaryTransferSpeed(r, RateUnit(u))
    }
  }
  
  operator fun plus(other: BinaryTransferSpeed): BinaryTransferSpeed {
    return plus(other.bytes)
  }
  
  operator fun plus(bytesPerSecond: Long): BinaryTransferSpeed {
    return BinaryTransferSpeed(this.bytes + bytesPerSecond)
  }
  
  operator fun minus(other: BinaryTransferSpeed): BinaryTransferSpeed {
    return minus(other.bytes)
  }
  
  operator fun minus(bytesPerSecond: Long): BinaryTransferSpeed {
    return BinaryTransferSpeed(this.bytes - bytesPerSecond)
  }
}
