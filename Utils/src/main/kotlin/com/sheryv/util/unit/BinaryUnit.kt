package com.sheryv.util.unit

enum class BinaryUnit(override val label: String, val shortLabel: String, val scale: Int) : MetricUnit {
  B("Bytes", "B", 0),
  KB("Kilobytes", "kB", 1),
  MB("Megabytes", "MB", 2),
  GB("Gigabytes", "GB", 3),
  TB("Terabytes", "TB", 4),
  PB("Petabytes", "PB", 5);
  
  override fun toString(): String = shortLabel
}
