package com.sheryv.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow


interface MetricUnit {
  val label: String
}

abstract class ScientificValue<V : Number, T : MetricUnit> {
  abstract val value: V
  abstract val unit: T
}

enum class BinaryUnit(override val label: String, val shortLabel: String, val scale: Int) : MetricUnit {
  B("Bytes", "B", 0), KB("Kilobytes", "kB", 1), MB("Megabytes", "MB", 2), GB("Gigabytes", "GB", 3), TB(
    "Terabytes", "TB", 4
  ),
  PB("Petabytes", "PB", 5);
  
  override fun toString(): String = shortLabel
}

class RateUnit<T : MetricUnit>(val base: T, override val label: String = base.label + "/sec.") : MetricUnit {
  override fun toString(): String = "$base/s"
}

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
    
    fun calc(value: Double, unit: BinaryUnit): BinarySize {
      val (r, b, u) = recalculate(value, unit)
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
  
  operator fun minus(other: BinarySize): BinarySize {
    return minus(other.bytes)
  }
  
  operator fun minus(bytes: Long): BinarySize {
    return calc(this.bytes.toDouble() - bytes, BinaryUnit.B)
  }
}

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
    
    fun calc(value: Double, unit: BinaryUnit): BinaryTransferSpeed {
      val (r, b, u) = recalculate(value, unit)
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

class FileSize(val sizeInBytes: Long, val formatter: DecimalFormat = DecimalFormat("# ##0.#")) {
  val sizeFormatted: String
  
  var size = 0.0
    private set
  
  var unit: String? = null
    private set
  
  fun full(): String {
    return sizeFormatted + unit
  }
  
  fun reformatOther(sizeInBytes: Long): FileSize {
    return FileSize(sizeInBytes, formatter)
  }
  
  init {
    require(sizeInBytes >= 0) { "Incorrect size $sizeInBytes" }
    val units = arrayOf("B", "kB", "MB", "GB", "TB", "PB")
    if (sizeInBytes == 0L) {
      size = 0.0
      unit = units[0]
      BinarySize.calc(23.0, BinaryUnit.KB) + 23
    } else {
      val digitGroups = (log10(sizeInBytes.toDouble()) / log10(1024.0)).toLong()
      size = sizeInBytes / 1024.0.pow(digitGroups.toDouble())
      unit = units[min(digitGroups.toDouble(), (units.size - 1).toDouble()).toInt()]
    }
    sizeFormatted = formatter.format(size)
  }
  
  companion object {
    fun format(sizeInBytes: Long): String {
      return FileSize(sizeInBytes).full()
    }
    
    val ZERO = FileSize(0)
  }
}
