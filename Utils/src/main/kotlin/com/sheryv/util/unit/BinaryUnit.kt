package com.sheryv.util.unit

enum class BinaryUnit(override val label: String, val shortLabel: String, val scale: Int) : MetricUnit {
  B("Bytes", "B", 0),
  KiB("Kibibytes", "KiB", 1),
  MiB("Mibibytes", "MiB", 2),
  GiB("Gibibytes", "GiB", 3),
  TiB("Tebibytes", "TiB", 4),
  PiB("Pebibytes", "PiB", 5);
  
  override fun toString(): String = shortLabel
}
