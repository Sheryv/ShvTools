package com.sheryv.util.unit

import java.text.DecimalFormat
import kotlin.math.pow

data class BinarySize(
  override val value: Double,
  override val unit: BinaryUnit,
  override val formatter: DecimalFormat = defaultFormatter
) : BinarySizeBase<BinaryUnit>() {
  
  override val bytes: Long = (1024.0.pow(unit.scale) * value).toLong()
  
  private constructor(bytes: Long) : this(bytes.toDouble(), BinaryUnit.B)
  
  companion object {
    @JvmStatic
    val zero = BinarySize(0.0, BinaryUnit.B)
    
    fun calc(value: Number, unit: BinaryUnit = BinaryUnit.B): BinarySize {
      val (r, b, u) = recalculate(value.toDouble(), unit)
      return BinarySize(r, u)
    }
    
    fun format(bytes: Long): String = calc(bytes.toDouble(), BinaryUnit.B).formatted
  }
  
  operator fun plus(other: BinarySize): BinarySize {
    return plus(other.bytes)
  }
  
  operator fun plus(bytes: Long): BinarySize {
    return calc(this.bytes.toDouble() + bytes, BinaryUnit.B)
  }
  
  operator fun plus(bytes: List<Long>): BinarySize {
    return calc(this.bytes.toDouble() + bytes.sum(), BinaryUnit.B)
  }
  
  operator fun minus(other: BinarySize): BinarySize {
    return minus(other.bytes)
  }
  
  operator fun minus(bytes: Long): BinarySize {
    return calc(this.bytes.toDouble() - bytes, BinaryUnit.B)
  }
}

fun Long.asBinarySize() = BinarySize.calc(this)
